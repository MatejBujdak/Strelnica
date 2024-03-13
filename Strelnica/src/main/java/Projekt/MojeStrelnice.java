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
import java.sql.SQLException;
import java.sql.Statement;

public class MojeStrelnice extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Connection con;
	String errorMessage = "";
	boolean usporiadaj = false;
	boolean filter = false;
	int filter_body = 0;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	doPost(request, response);
	}

    ///// POST /////
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		HttpSession session = request.getSession();
		con = (Connection)session.getAttribute("spojenie"); 
    	response.setContentType("text/html;charset=UTF-8");
    	out.println("<head>");
    	out.println("<link rel='stylesheet' type='text/css' href='styles.css'>");
    	out.println("</head>");
    	try {
    		
            String operacia = request.getParameter("operacia");
            if (badConnection(out)) return;
            
            //kontrola id
            int user_id = getUserID(request);
            if (user_id == 0) { zobrazNeopravnenyPristup(out); return; }
            
            //operacie
            if(operacia != null) {
            if (operacia.equals("admin") && overAdmina(request) == -1) {  zobrazNeopravnenyPristup(out); return; }
            if (operacia.equals("nastav_pouzivatela") && nastav_pouzivatela(request) == -1) { zobrazNeopravnenyPristup(out); return; }
            else if (operacia.equals("vymazat")) { odobratPolozku(Integer.parseInt(request.getParameter("id_vymazanie")), out, request); }
            else if (operacia.equals("editovat")) { editovatRezervaciu(Integer.parseInt(request.getParameter("id_editovat")), out, request); }
            else if (operacia.equals("pridaj_kolo")) { pridajKolo(Integer.parseInt(request.getParameter("id_pridaj")), out, request); }
            else if (operacia.equals("uprav_body")) { upravBody(Integer.parseInt(request.getParameter("id_kola")), out, request); }
            else if (operacia.equals("filtrovat")) { nasFilt(request); }
            else if (operacia.equals("usporiadat_datum")) { usporiadaj(); }
            }
            
            //vypis stranky
            vypisHeader(out, request);
            vypisBody(out, user_id, request);
            createFooter(out, request);
            
        } catch (Exception e) {  out.println(e); }
	}	


	private void usporiadaj() {
		if (usporiadaj) {
		    usporiadaj = false;
		} else {
		    usporiadaj = true;
		}
	}

	protected void nasFilt(HttpServletRequest request) {
		filter = true;
		filter_body = Integer.parseInt(request.getParameter("filt_body"));
	}
    
    //KOLA UPRAVA
    private void upravBody(int id, PrintWriter out, HttpServletRequest request) {
    	try {
            int body = Integer.parseInt(request.getParameter("body"));
            String typ = request.getParameter("typ");

            Statement stmt = con.createStatement();
            String sql = "UPDATE rounds SET pocet_bodov = " + body + ", typ = '" + typ + "' WHERE id = " + id;
            stmt.executeUpdate(sql); 

        } catch (SQLException e) {
            out.println("SQL Chyba při editaci rezervace: " + e.getMessage());
        }
	}

	//PRIDAJ KOLO
    private void pridajKolo(int parseInt, PrintWriter out, HttpServletRequest request) {

    	 PreparedStatement pstmt = null;
    	 ResultSet rs = null;
    	    
        try {
        	
        	String typ = request.getParameter("typ");
            int id_rezer = Integer.parseInt(request.getParameter("id_rezer"));
            int id_pouz = Integer.parseInt(request.getParameter("id_pouz"));
        	int body = Integer.parseInt(request.getParameter("pocet_bodov"));
            
        	String sql = "SELECT MAX(kolo) AS maxKolo FROM rounds WHERE id_users = ? AND id_strelnice = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, id_pouz);
            pstmt.setInt(2, id_rezer);
            rs = pstmt.executeQuery();

            int noveKolo = 1; // Výchozí hodnota pro nové kolo
            if (rs.next()) {
                noveKolo = rs.getInt("maxKolo") + 1;
            }
            
            sql = "INSERT INTO rounds (typ, id_strelnice, id_users, kolo, pocet_bodov) VALUES (?, ?, ?, ?, ?)";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, typ);
            pstmt.setInt(2, id_rezer);
            pstmt.setInt(3, id_pouz);
            pstmt.setInt(4, noveKolo);
            pstmt.setInt(5, body);

            pstmt.executeUpdate();
        }catch (SQLException e) {
            out.println("SQL Chyba při přidávání kola: " + e.getMessage());
        }
        
        
	}

	private void editovatRezervaciu(int id, PrintWriter out, HttpServletRequest request) {
        try {
            int doba = Integer.parseInt(request.getParameter("doba"));
            String datum = request.getParameter("datum");

            Statement stmt = con.createStatement();
            String sql = "UPDATE prenajom SET doba_prenajatia = " + doba + ", datum = '" + datum + "' WHERE id = " + id;
            stmt.executeUpdate(sql); // Zde použijte executeUpdate místo executeQuery

        } catch (SQLException e) {
            out.println("SQL Chyba při editaci rezervace: " + e.getMessage());
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
		
		
		protected int getUserID(HttpServletRequest request) {
			HttpSession session = request.getSession();
			Integer id = (Integer)(session.getAttribute("ID"));
			if(id==null) id = 0;
			return id;  
		}
		
///////////////////////////////////////////////////////	 /////////////////////

		//// BODY ////
		protected void vypisBody(PrintWriter out, int id_user, HttpServletRequest request) {

		try {
		Statement stmt= con.createStatement();
		String sql;
		if(usporiadaj) {
			 sql = "SELECT * FROM prenajom INNER JOIN strelnice ON prenajom.id_strelnica = strelnice.id WHERE prenajom.id_pouzivatel = " + id_user + " ORDER BY prenajom.datum";
		}else {
			sql = "SELECT * FROM prenajom INNER JOIN strelnice ON prenajom.id_strelnica = strelnice.id WHERE prenajom.id_pouzivatel = " + id_user;

		}
		ResultSet rs = stmt.executeQuery(sql);
		
		HttpSession session = request.getSession();
		
		if (((int) session.getAttribute("majitel")) == 1) {
			out.println("<form method='post' action='Strelnice'>");
			out.println("<input type='submit' value='strelnice'>");
			out.println("</form>");
		}
		
		out.println("<form method='post' action='MojeStrelnice'>");
		out.println("<input type='hidden' name='operacia' value='usporiadat_datum'>");
		out.println("<input type='submit' value='Usporiadat podla datumu'>");
		out.println("</form>");
		
		out.println("<form method='post' action='MojeStrelnice'>");
		out.println("<input type='hidden' name='operacia' value='filtrovat'>");
		out.println("Zadaj počet hodin rezervacie: <input type='number' name='filt_body' required>");
		out.println("<input type='submit' value='filtrovat podla hodin rezervacie'>");
		out.println("</form>");
		
		out.println("<hr>");
		while (rs.next()) {
			if(!filter || Integer.parseInt(rs.getString("doba_prenajatia")) == filter_body) {
			//REZERVACIA
	        out.println("<form action='MojeStrelnice' method='post'>");
	        out.println("<input type='hidden' name='id_editovat' value='" + rs.getString("ID") + "'>");
	        out.println("<input type='hidden' name='operacia' value='editovat'>");
	        out.println("<input type='submit' value='editovat'>");
	        out.println("Nazov strelnice: <strong>" + rs.getString("strelnice.nazov") + ": </strong>");
	        out.println("Počet hodín: <input type='number' name='doba' value='" + rs.getString("doba_prenajatia") + "' required>");
	        out.println("&nbsp;&nbsp;Dátum prenajatia: <input type='text' name='datum' value='" + rs.getString("datum") + "' required>");
	        out.println("</form>");     
	        
	        Statement stmt2 = con.createStatement();
	        sql = "SELECT * FROM rounds WHERE id_users = " + id_user + " AND id_strelnice = " + rs.getInt("id_strelnica");
	        ResultSet rs2 = stmt2.executeQuery(sql);
	        
	        while (rs2.next()) {
		        out.println("<form action='MojeStrelnice' method='post'>");
		        out.println("<input type='hidden' name='id_kola' value='" + rs2.getString("id") + "'>");
		        out.println("<input type='hidden' name='operacia' value='uprav_body'>");
		        out.println("Kolo:" + rs2.getString("kolo") + ": ");
		        out.println("Typ: " + "<input type='text' name='typ' value='" + rs2.getString("typ") + "' required>");
		        out.println("Počet bodov: <input type='number' name='body' value='" + rs2.getString("pocet_bodov") + "' required>");
		        out.println("<input type='submit' value='upravit'>");
		        out.println("</form>");
	        }
	        
	        //PRIDAJ KOLO
	        out.println("<form action='MojeStrelnice' method='post'>");
	        out.println("<input type='hidden' name='id_pridaj' value='" + rs.getString("ID") + "'>");
	        out.println("<input type='hidden' name='operacia' value='pridaj_kolo'>");
	        out.println("<input type='hidden' name='id_rezer' value='"+rs.getString("id_strelnica")+"'>");
	        out.println("<input type='hidden' name='id_pouz' value='"+id_user+"'>");
	        out.println("Typ strelby: <input type='text' name='typ' required>");
	        out.println("&nbsp;&nbsp; Počet bodov: <input type='number' name='pocet_bodov' required>");
	        out.println("<input type='submit' value='pridat kolo'>");
	        out.println("</form>");
	        
	        //VYMAZAT REZERVACIU
	        out.println("<form action='MojeStrelnice' method='post'>");
	        out.println("<input type='hidden' name='id_vymazanie' value='" + rs.getString("ID") + "'>");
	        out.println("<input type='hidden' name='operacia' value='vymazat'>");
	        out.println("<input type='submit' value='vymazat rezervaciu'>");
	        out.println("</form> <br>");
	      }
		out.println("</div>");

		}
		rs.close();
		stmt.close();
		}catch (Exception ex) {
			out.println(ex.getMessage());
		}
		}
		
		
		private void odobratPolozku(int id, PrintWriter out, HttpServletRequest request) {
		    Statement stmt = null;
		    try {
		        stmt = con.createStatement();
		        System.out.println(id);
		        
		        String sql = "DELETE FROM rounds WHERE id_strelnice = " + id;
		        stmt.executeUpdate(sql);

		        sql = "DELETE FROM prenajom WHERE id_strelnica = " + id;
		        stmt.executeUpdate(sql);

		    } catch (Exception e) {
		        out.println("Chyba při odstraňování položky: " + e.getMessage());
		    } finally {
		        if (stmt != null) {
		            try {
		                stmt.close();
		            } catch (SQLException e) {
		                out.println("Chyba při uzavírání Statementu: " + e.getMessage());
		            }
		        }
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

			// logout
			out.println("<form method='post' action='StrelnicaMain'>");
			out.println("<input type='hidden' name='operacia' value='logout'>");
			out.println("<input class='logout-button' type='submit' value='logout'>");
			out.println("</form>");

			out.println("</div>");

		}
		

	}

