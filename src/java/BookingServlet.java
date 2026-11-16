import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/BookingServlet"})
public class BookingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect("index.html");
            return;
        }

        String currentUser = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        try (Connection conn = DBUtil.getConnection()) {

            if ("book".equals(action)) {
                String venueName = request.getParameter("venueName").trim().toUpperCase();
                String equipmentNeeded = request.getParameter("equipmentNeeded");
                equipmentNeeded = (equipmentNeeded == null || equipmentNeeded.trim().isEmpty())
                        ? "NONE"
                        : equipmentNeeded.trim().toUpperCase();
                String bookingDateStr = request.getParameter("bookingDate");
                String timeSlot = request.getParameter("timeSlot").trim();
                String purpose = request.getParameter("purpose");
                purpose = (purpose == null || purpose.trim().isEmpty()) ? "-" : purpose.trim();

                Date sqlDate = Date.valueOf(bookingDateStr);

                // Check if same venue, date, and time slot is already booked
                String conflictSql = "SELECT booking_id, booked_by FROM bookings "
                                   + "WHERE UPPER(venue_name) = ? AND booking_date = ? AND time_slot = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(conflictSql)) {
                    checkStmt.setString(1, venueName);
                    checkStmt.setDate(2, sqlDate);
                    checkStmt.setString(3, timeSlot);

                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            session.setAttribute("bookingMessage", "Venue is occupied (" + venueName + " is already booked on " + bookingDateStr + " during " + timeSlot + ").");
                            session.setAttribute("bookingMsgType", "error");
                            response.sendRedirect("DashboardServlet?tab=bookings");
                            return;
                        }
                    }
                }

                // No conflict: insert new booking
                String insertSql = "INSERT INTO bookings (venue_name, equipment_needed, booking_date, time_slot, purpose, booked_by) "
                                 + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, venueName);
                    insertStmt.setString(2, equipmentNeeded);
                    insertStmt.setDate(3, sqlDate);
                    insertStmt.setString(4, timeSlot);
                    insertStmt.setString(5, purpose);
                    insertStmt.setString(6, currentUser);
                    insertStmt.executeUpdate();
                }

                session.setAttribute("bookingMessage", "Booking confirmed for " + venueName + " on " + bookingDateStr + " (" + timeSlot + ").");
                session.setAttribute("bookingMsgType", "success");

            } else if ("cancel".equals(action)) {
                int bookingId = Integer.parseInt(request.getParameter("bookingId"));

                // Admin can cancel any booking; standard users can cancel their own
                String delSql = "admin".equals(currentUser)
                        ? "DELETE FROM bookings WHERE booking_id = ?"
                        : "DELETE FROM bookings WHERE booking_id = ? AND booked_by = ?";

                try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                    delStmt.setInt(1, bookingId);
                    if (!"admin".equals(currentUser)) {
                        delStmt.setString(2, currentUser);
                    }
                    int rows = delStmt.executeUpdate();
                    if (rows > 0) {
                        session.setAttribute("bookingMessage", "Booking #" + bookingId + " has been cancelled.");
                        session.setAttribute("bookingMsgType", "success");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("bookingMessage", "Booking error: " + e.getMessage());
            session.setAttribute("bookingMsgType", "error");
        }

        String returnTab = request.getParameter("returnTab");
        if ("summary".equals(returnTab)) {
            response.sendRedirect("DashboardServlet?tab=summary");
        } else {
            response.sendRedirect("DashboardServlet?tab=bookings");
        }
    }
}