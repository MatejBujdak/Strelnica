package Projekt;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class StrelnicaMain extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
		//CONNECTION
	    private String URL = "jdbc:mysql://localhost:3307/";
	    private String databaza = "strelnica";
	    private String userName = "root";
	    private String pass = "";
	    private Connection con;
	    private String errorMessage = "";
	    
	    //GUARD
	    public class Guard implements HttpSessionBindingListener {
	  	  Connection connection;
	  	 public Guard(Connection c) {
	  	    connection = c; 
	  	 }
	  	 @Override
	  	 public void valueBound(HttpSessionBindingEvent event) {}
	  	 @Override
	  	 public void valueUnbound(HttpSessionBindingEvent event) {
	  	    try { 
	  	       if (connection != null) connection.close(); 
	  	    } catch (Exception e) { errorMessage += e;}
	  	  }            
	  	}
	    
	    //DAJ SPOJENIE
	    protected Connection dajSpojenie(HttpServletRequest request) {
	        try {
	         HttpSession session = request.getSession();
	          con = (Connection)session.getAttribute("spojenie"); 
	          if (con == null) { 
	        	Class.forName("com.mysql.cj.jdbc.Driver");
	        	con = DriverManager.getConnection(URL + databaza, userName, pass);
	            session.setAttribute("spojenie", con);
	            new Guard(con);
	          } 
	          return con; 
	        } catch(Exception e) {
	        	errorMessage += e;
	        	return null;
	        }     
	      }
	/////////////////////////////
	    
	    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    	doPost(request, response);
		}

	    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			PrintWriter out = response.getWriter();
			response.setContentType("text/html;charset=UTF-8");
			dajSpojenie(request);
	    	out.println("<head>");
	    	out.println("<link rel='stylesheet' type='text/css' href='styles.css'>");
	    	out.println("</head>");
	    	
	    	try {
	    		
	    		//overenie pouzivatela
	            String operacia = request.getParameter("operacia");
	            if (badConnection(out) || badOperation(operacia, out)) return;
	            
	            if (operacia.equals("login")) { overUser(out, request); }
	            else if(operacia.equals("sign_up")) { regUser(out, request); }
	            
	            //kontrola id
	            int user_id = getUserID(request);
	            if (user_id == 0) { zobrazNeopravnenyPristup(out); return; }
	            
	            //operacie
	            if (operacia.equals("logout")) { urobLogout(out, request); return; }
	            if (operacia.equals("admin") && overAdmina(request) == -1) {  zobrazNeopravnenyPristup(out); return; }
	            if (operacia.equals("nastav_pouzivatela") && nastav_pouzivatela(request) == -1) { zobrazNeopravnenyPristup(out); return; }
	            if (operacia.equals("objednat")) { objednat(request, out); }
	            
	            //vypis stranky
	            vypisHeader(out, request);
	            vypisBody(out, request);
	            createFooter(out, request);
	            
	        } catch (Exception e) {  out.println(e); }
		}
	    
	    protected void objednat(HttpServletRequest request, PrintWriter out) {
	        try {
	            // Get booking details from request
	            String datumCas = request.getParameter("datumCas");
	            String doba = request.getParameter("doba");
	            int userID = getUserID(request); // Assuming this returns the user's ID
	            int strelnicaID = Integer.parseInt(request.getParameter("ID")); // Get the ID of the strelnica

	            // SQL to insert booking details into prenajom table
	            String insertSQL = "INSERT INTO prenajom (id_pouzivatel, id_strelnica, datum, doba_prenajatia, pocet_bodov) VALUES (?, ?, ?, ?, 0)";
	            PreparedStatement pstmt = con.prepareStatement(insertSQL);

	            pstmt.setInt(1, userID);
	            pstmt.setInt(2, strelnicaID);
	            pstmt.setString(3, datumCas);
	            pstmt.setString(4, doba);

	            // Execute the insert operation
	            int rowsAffected = pstmt.executeUpdate();
	            if (rowsAffected > 0) {
	                out.println("pridanie uspesne");
	            } else {
	                out.println("chyba");
	            }
	        } catch (Exception e) {
	            out.println("Error during booking: " + e.getMessage());
	        }
	    }
		
	    //overenie pouzivatela
	    private boolean badConnection(PrintWriter out) {
	        if (errorMessage.length() > 0) {
	            out.println(errorMessage);
	            return true;
	        }
	        return false;
	    }
	    private boolean badOperation(String operacia, PrintWriter out) {
	    	   if (operacia == null) {
	    		   zobrazNeopravnenyPristup(out);
	    	       return true;
	    	   }
	    	   return false;
	    	}
		protected void zobrazNeopravnenyPristup(PrintWriter out) {
			out.println("Neopravneny pristup");
		}
	    
		protected void regUser(PrintWriter out, HttpServletRequest request) {
		    try {
		        Statement stmt = con.createStatement();

		        String login = request.getParameter("login");
		        String heslo = request.getParameter("pwd");
		        String meno = request.getParameter("meno");
		        String priezvisko = request.getParameter("priezvisko");

		        // overenie loginu
		        String checkUserExist = "SELECT * FROM users WHERE login = '" + login + "'";
		        ResultSet userExist = stmt.executeQuery(checkUserExist);

		        if (!platnyEmail(login)) {
		            out.println("Neplatný formát emailovej adresy.");
		            return;
		        }else if (userExist.next()) {
		            out.println("Používateľ s rovnakým loginom už existuje.");
		        } else {
		            String insertUser = "INSERT INTO users(login, passwd, meno, priezvisko, majitel) VALUES ('" + login + "', '" + heslo + "', '" + meno + "', '" + priezvisko + "', 0)";
		            stmt.executeUpdate(insertUser);
		            overUser(out, request);
		            out.println("Registrácia úspešná!");
		        }
		    } catch (Exception e) {
		        out.println(e.getMessage());
		    }
		}

		private boolean platnyEmail(String email) {
		    String regex = "^(.+)@(\\S+)\\.(\\S+)$";
		    return email.matches(regex);
		}
			
		protected void overUser(PrintWriter out, HttpServletRequest request) {
			try {
				String login = request.getParameter("login");
				String heslo = request.getParameter("pwd");
				Statement stmt = con.createStatement();
				String sql = "SELECT MAX(id) AS iid, COUNT(id) AS pocet FROM users WHERE login='"+login+"' AND passwd = '" + heslo + "'";
				ResultSet rs = stmt.executeQuery(sql);
				rs.next();
				HttpSession session = request.getSession();
				if(rs.getInt("pocet") == 1) {
					sql = "SELECT * FROM users WHERE login = '"+login+"'";
					rs = stmt.executeQuery(sql);
					rs.next();
					session.setAttribute("ID", rs.getInt("ID"));
					session.setAttribute("login", rs.getString("login"));
					session.setAttribute("meno", rs.getString("meno"));
					session.setAttribute("priezvisko", rs.getString("priezvisko"));
					session.setAttribute("majitel", rs.getInt("majitel"));
				} else {
					out.println("Autorizacia sa nepodarila. Skontroluj prihlasovacie udaje.");
					session.invalidate();
				}
				rs.close();
				stmt.close();
			}catch(Exception e) {out.println(e.getMessage());} 
		}
		
		protected int getUserID(HttpServletRequest request) {
			HttpSession session = request.getSession();
			Integer id = (Integer)(session.getAttribute("ID"));
			if(id==null) id = 0;
			return id;  
		}
		
///////////////////////////////////////////////////////	 

		//// BODY ////
		protected void vypisBody(PrintWriter out, HttpServletRequest request) {

		try {
		Statement stmt= con.createStatement();
		String sql = "SELECT * FROM strelnice ";
		ResultSet rs = stmt.executeQuery(sql);
		out.println("<hr>");
		while (rs.next()) {
		    out.println("<form action='StrelnicaMain' method='post'>");
		    out.println("<input type='hidden' name='ID' value='" + rs.getString("ID") + "'>");
		    out.println("<input type='hidden' name='operacia' value='objednat'>");
		    out.println("datum: <input type='datetime-local' name='datumCas' required>");
		    out.println("pocet hodin: <input type='number' name='doba' required>");
		    out.println("<input type='submit' value='Prenajat'>");
		    out.println("&nbsp;&nbsp;" + rs.getString("nazov") +
		            ":</strong>" + rs.getString("adresa"));
		    
		    out.println("</form>");
		}
		out.println("</div>");
			rs.close();
			stmt.close();
		}catch (Exception ex) {
			out.println(ex.getMessage());
		}
		}
		
		//// FOOTER ////

		private void createFooter(PrintWriter out, HttpServletRequest request) {
			out.println("<a class='footer-link' href='MojeStrelnice'>Moje strelnice</a>");
		}
		
		protected int overAdmina(HttpServletRequest request) {
			HttpSession session = request.getSession();
			session.setAttribute("admin_status", "true");
			if(((int)session.getAttribute("majitel") == 1)) {
				return 1;
			}
			return -1;
		}
		
		protected int nastav_pouzivatela(HttpServletRequest request) {
			HttpSession session = request.getSession();
			session.setAttribute("admin_status", "false");
			if(((int)session.getAttribute("majitel") == 1)) {
				return 1;
			}
			return -1;
		}
		
		protected void urobLogout(PrintWriter out, HttpServletRequest request) {
			HttpSession session = request.getSession();
			session.invalidate();
			out.println("Odhlasenie bolo uspešné.<br>");
			out.println("<a href='index.html'>prihlasiť sa</a>");
		}
		
		//// HEADER ////
		protected void vypisHeader(PrintWriter out, HttpServletRequest request) {
			HttpSession session = request.getSession();
			// Administratorske tlacidla

			// logout
			out.println("<form method='post' action='StrelnicaMain'>");
			out.println("<input type='hidden' name='operacia' value='logout'>");
			out.println("<input class='logout-button' type='submit' value='logout'>");
			out.println("</form>");

			out.println("</div>");

		}
		

	}

