import java.sql.*;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Vector;

class DBHelper {
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found: " + e.getMessage());
        }
        String url = "jdbc:mysql://127.0.0.1:3306/ice_cream_management?useSSL=false&serverTimezone=UTC";
        String username = "local_user";  
        String password = ""; 
        
        return DriverManager.getConnection(url, username, password);
    }
}

class IceCreamService {
    final String ADMIN_PASSWORD = "siws";  
    private Vector<Order> orderHistory = new Vector<>();  

    class Order {
        String name;
        String type;
        int quantity;
        String address;
        Timestamp orderTime;

        Order(String name, String type, int quantity, String address, Timestamp orderTime) {
            this.name = name;
            this.type = type;
            this.quantity = quantity;
            this.address = address;
            this.orderTime = orderTime;
        }

        @Override
        public String toString() {
            return "Customer: " + name + " | Ordered: " + type + " | Quantity: " + quantity + 
                   " | Address: " + address + " | Order Time: " + orderTime;
        }
    }

    public void placeOrder(String name, int choice, int quantity, String address) {
        try (Connection conn = DBHelper.getConnection()) {
            String selectQuery = "SELECT type, price, stock FROM ice_cream WHERE id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setInt(1, choice);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String type = rs.getString("type");
                int price = rs.getInt("price");
                int stock = rs.getInt("stock");

                if (stock < quantity) {
                    System.out.println("Sorry, we only have " + stock + " left in stock.");
                    return;
                }

                String updateStockQuery = "UPDATE ice_cream SET stock = stock - ? WHERE id = ?";
                PreparedStatement updateStockStmt = conn.prepareStatement(updateStockQuery);
                updateStockStmt.setInt(1, quantity);
                updateStockStmt.setInt(2, choice);
                updateStockStmt.executeUpdate();

                String insertOrderQuery = "INSERT INTO customer_orders (name, order_type, quantity, address, order_time) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertOrderStmt = conn.prepareStatement(insertOrderQuery);
                insertOrderStmt.setString(1, name);
                insertOrderStmt.setString(2, type);
                insertOrderStmt.setInt(3, quantity);
                insertOrderStmt.setString(4, address);
                insertOrderStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                insertOrderStmt.executeUpdate();

                int totalAmount = price * quantity;
                System.out.println("Order placed successfully! Total Bill: Rs " + totalAmount);

                orderHistory.add(new Order(name, type, quantity, address, Timestamp.valueOf(LocalDateTime.now())));
            } else {
                System.out.println("Invalid choice.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePrice(int choice, int newPrice) {
        try (Connection conn = DBHelper.getConnection()) {
            String query = "UPDATE ice_cream SET price = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, newPrice);
            stmt.setInt(2, choice);
            stmt.executeUpdate();
            System.out.println("Price updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateStock(int choice, int newStock) {
        try (Connection conn = DBHelper.getConnection()) {
            String query = "UPDATE ice_cream SET stock = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, newStock);
            stmt.setInt(2, choice);
            stmt.executeUpdate();
            System.out.println("Stock updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void viewCustomersAndOrders() {
        if (orderHistory.isEmpty()) {
            System.out.println("No orders yet.");
        } else {
            for (Order order : orderHistory) {
                System.out.println(order.toString());
            }
        }
    }

    public void viewStockAndPrices() {
        try (Connection conn = DBHelper.getConnection()) {
            String query = "SELECT * FROM ice_cream";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString("type") + " - Price: Rs " + rs.getInt("price") + " | Stock: " + rs.getInt("stock"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean validateAdminPassword(String password) {
        return password.equals(ADMIN_PASSWORD);
    }
}

class IceCreamManagementUI {
    IceCreamService service;
    Scanner sc = new Scanner(System.in);

    IceCreamManagementUI(IceCreamService service) {
        this.service = service;
    }

    public void displayMenu() {
        try (Connection conn = DBHelper.getConnection()) {
            String query = "SELECT * FROM ice_cream";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            System.out.println("********** Welcome to Ice Cream Palace **********");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ". " + rs.getString("type") +
                        " - Rs " + rs.getInt("price") + " (Stock: " + rs.getInt("stock") + ")");
            }
            System.out.println("4. Exit");
            System.out.println("==================================================");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void takeCustomerOrder() {
        sc.nextLine();
        System.out.print("Enter your name: ");
        String name = sc.nextLine();
        System.out.print("Enter your address: ");
        String address = sc.nextLine();

        while (true) {
            displayMenu();
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice < 1 || choice > 4) {
                System.out.println("Invalid choice. Please enter a valid ice cream ID (1-4).");
                continue;
            }

            if (choice == 4) {
                break;
            }

            System.out.print("Enter the quantity: ");
            int quantity = sc.nextInt();

            service.placeOrder(name, choice, quantity, address);

            String moreOrder;
            do {
                System.out.print("Do you want to order more ice creams? (yes/no or y/n): ");
                moreOrder = sc.next().trim().toLowerCase();

                if (!(moreOrder.equals("yes") || moreOrder.equals("no") || moreOrder.equals("y") || moreOrder.equals("n"))) {
                    System.out.println("Invalid input. Please enter either 'yes', 'no', 'y', or 'n'.");
                }
            } while (!(moreOrder.equals("yes") || moreOrder.equals("no") || moreOrder.equals("y") || moreOrder.equals("n")));

            if (moreOrder.equals("no") || moreOrder.equals("n")) {
                break;
            }
        }
    }

    public void adminMode() {
        System.out.print("Enter admin password: ");
        String enteredPassword = sc.next();

        if (!service.validateAdminPassword(enteredPassword)) {
            System.out.println("Incorrect password. Access denied.");
            return;
        }

        System.out.println("Access granted to Admin Mode.");
        while (true) {
            System.out.println("Admin Menu:");
            System.out.println("1. View Customers and Orders");
            System.out.println("2. Update Prices");
            System.out.println("3. Update Stock");
            System.out.println("4. View Stock and Prices");
            System.out.println("5. Exit Admin Mode");
            System.out.print("Enter your choice: ");
            int adminChoice = sc.nextInt();

            switch (adminChoice) {
                case 1 -> service.viewCustomersAndOrders();
                case 2 -> updatePrices();
                case 3 -> updateStock();
                case 4 -> service.viewStockAndPrices();
                case 5 -> {
                    System.out.println("Exiting Admin Mode...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void updatePrices() {
        System.out.print("Enter ice cream ID to update price: ");
        int choice = sc.nextInt();
        System.out.print("Enter new price: ");
        int newPrice = sc.nextInt();
        service.updatePrice(choice, newPrice);
    }

    private void updateStock() {
        System.out.print("Enter ice cream ID to update stock: ");
        int choice = sc.nextInt();
        System.out.print("Enter new stock: ");
        int newStock = sc.nextInt();
        service.updateStock(choice, newStock);
    }
}

public class V3_18041 {
    public static void main(String[] args) {
        IceCreamService service = new IceCreamService();
        IceCreamManagementUI ui = new IceCreamManagementUI(service);

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Select Mode: ");
            System.out.println("1. Customer Mode");
            System.out.println("2. Admin Mode");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");
            int mode = sc.nextInt();

            switch (mode) {
                case 1 -> ui.takeCustomerOrder();
                case 2 -> ui.adminMode();
                case 3 -> {
                    System.out.println("Thank you for visiting Ice Cream Palace!");
                    System.exit(0);
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
