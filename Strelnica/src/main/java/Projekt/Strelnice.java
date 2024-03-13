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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Strelnice extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Connection con;
	String errorMessage = "";

    ///// POST /////
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
            if (operacia.equals("upravit_s")) { upravit_s(Integer.parseInt(request.getParameter("id_s")), out, request); }
            if (operacia.equals("odstran")) { odstran(Integer.parseInt(request.getParameter("id_s")), out, request); }
            if (operacia.equals("pridat")) { pridaj(out, request);};
            }
            
            //vypis stranky
            vypisHeader(out, request);
            vypisBody(user_id, out, request);

            
        } catch (Exception e) {  out.println(e); }
	}
    
    
    private void odstran(int id, PrintWriter out, HttpServletRequest request) {
    	try {

            Statement stmt = con.createStatement();
            String sql = "DELETE FROM strelnice WHERE id = " + id;
            
            stmt.executeUpdate(sql); 

        } catch (SQLException e) {
            out.println("SQL chyba upravit_s " + e.getMessage());
        }
	}

	private void pridaj(PrintWriter out, HttpServletRequest request) {
    	try {
    		String nazov = request.getParameter("add_nazov");
            String adresa = request.getParameter("add_adresa");

            Statement stmt = con.createStatement();
            String sql = "INSERT INTO strelnice (nazov, adresa) VALUES ('" + nazov + "', '" + adresa + "')";

            
            stmt.executeUpdate(sql); 

        } catch (SQLException e) {
            out.println("SQL chyba upravit_s " + e.getMessage());
        }
}
    
	    private void upravit_s(int id, PrintWriter out, HttpServletRequest request) {
	    	try {
	    		String nazov = request.getParameter("nazov");
	            String adresa = request.getParameter("adresa");

	            Statement stmt = con.createStatement();
	            String sql = "UPDATE strelnice SET nazov = '" + nazov + "', adresa = '" + adresa + "' WHERE id = " + id;
	            
	            stmt.executeUpdate(sql); 

	        } catch (SQLException e) {
	            out.println("SQL chyba upravit_s " + e.getMessage());
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
		
///////////////////////////////////////////////////////	 

		//// BODY ////
		protected void vypisBody( int id_user, PrintWriter out, HttpServletRequest request) {

		try {
		Statement stmt= con.createStatement();
		String sql = "SELECT * FROM strelnice";
		ResultSet rs = stmt.executeQuery(sql);
		
		HttpSession session = request.getSession();
		
		while (rs.next()) {
			
			//UPRAV
	        out.println("<form action='Strelnice' method='post'>");
	        out.println("<input type='hidden' name='id_s' value='" + rs.getString("id") + "'>");
	        out.println("Nazov strelnice: <input type='text' name='nazov' value='" + rs.getString("nazov") + "' required>");
	        out.println("&nbsp;&nbsp; adresa: <input type='text' name='adresa' value='" + rs.getString("adresa") + "' required>");
	        out.println("<input type='hidden' name='operacia' value='upravit_s'>");
	        out.println("<input type='submit' value='editovat'>"); 
	        out.println("</form>");
	        
	        //ODSTRAN
	        out.println("<form action='Strelnice' method='post'>");
	        out.println("<input type='hidden' name='operacia' value='odstran'>");
	        out.println("<input type='hidden' name='id_s' value='" + rs.getString("id") + "'>");
	        out.println("<input type='submit' value='odstranit'>"); 
	        out.println("</form>");
	        
	        out.println("<hr>");
	
	      }
		
		out.println("<form action='Strelnice' method='post'>");
        out.println("<input type='hidden' name='operacia' value='pridat'>");
        out.println("Nazov strelnice: <input type='text' name='add_nazov' required>");
        out.println("&nbsp;&nbsp; adresa: <input type='text' name='add_adresa' required>");
        out.println("<input type='submit' value='pridat'>"); 
        out.println("</form>");
      
		out.println("</div>");
			rs.close();
			stmt.close();
		}catch (Exception ex) {
			out.println(ex.getMessage());
		}
		}
		

		
		//// FOOTER ////
		
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

