package supermarket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JComboBox;
import javax.swing.table.DefaultTableModel;

import java.sql.ResultSetMetaData;


public class DatabaseOperations {
    // declaring connection and dataSource variables
    private static Connection conn;

    // initialize method to initialize the database with all the tables
    public static void dbInit() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
            createTables();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void createTables() throws SQLException {
        Statement statement = conn.createStatement();

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS customers ("
                + "id INT PRIMARY KEY AUTO_INCREMENT,"
                + "name VARCHAR(100) NOT NULL,"
                + "phone VARCHAR(20) NOT NULL,"
                + "email VARCHAR(100),"
                + "address VARCHAR(255)"
                + ");");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS products ("
                + "id INT PRIMARY KEY AUTO_INCREMENT,"
                + "name VARCHAR(100) NOT NULL,"
                + "price DECIMAL(10,2) NOT NULL"
                + ");");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS orders ("
                + "id INT PRIMARY KEY AUTO_INCREMENT,"
                + "customer_id INT NOT NULL,"
                + "date DATE NOT NULL,"
                + "FOREIGN KEY (customer_id) REFERENCES customers(id)"
                + ");");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS order_items ("
                + "id INT PRIMARY KEY AUTO_INCREMENT,"
                + "order_id INT NOT NULL,"
                + "product_id INT NOT NULL,"
                + "quantity INT NOT NULL,"
                + "price DECIMAL(10,2) NOT NULL,"
                + "FOREIGN KEY (order_id) REFERENCES orders(id),"
                + "FOREIGN KEY (product_id) REFERENCES products(id)"
                + ");");

        statement.close();
    }

    /*
     * ----------------------------------- Order Operations--------------------------------------------------
     */

    // Method to create new orders
    public static int createNewOrder(int custID) throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        PreparedStatement ps = conn.prepareStatement("INSERT INTO orders (customer_id, date) VALUES (?, CURDATE())",
                PreparedStatement.RETURN_GENERATED_KEYS);

        ps.setInt(1, custID);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        int oid = rs.getInt(1);

        rs.close();
        ps.close();
        conn.close();
        return oid;
    }

    // Method to add new items to the order
    public static void addOrderItems(int orderID, int prodID, int quantity, Float price) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)")) {

            ps.setInt(1, orderID);
            ps.setInt(2, prodID);
            ps.setInt(3, quantity);
            ps.setFloat(4, price);

            ps.executeUpdate();
        } // Resources will be closed automatically here
    }


    // Method to discard/delete the order and the ordered items
    public static void discardOrder(int orderID) throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        String sql = "DELETE  FROM order_items WHERE order_id = ?;\n";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, orderID);
        ps.executeUpdate();
        ps = conn.prepareStatement("DELETE  FROM orders WHERE id = ?;");
        ps.setInt(1, orderID);
        ps.executeUpdate();
        ps.close();

        conn.close();
    }

    // Method to delete items from the order_items table
    public static void deleteOrderItem(int orderID) throws SQLException {
    	conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        // Retrieve the maximum id for the given order_id
        String getMaxIdQuery = "SELECT MAX(id) FROM order_items WHERE order_id = ?";
        try (PreparedStatement getMaxIdStatement = conn.prepareStatement(getMaxIdQuery)) {
            getMaxIdStatement.setInt(1, orderID);
            try (ResultSet maxIdResult = getMaxIdStatement.executeQuery()) {
                int maxId = 0;

                if (maxIdResult.next()) {
                    maxId = maxIdResult.getInt(1);

                    // Now delete the record with the retrieved id
                    String deleteQuery = "DELETE FROM order_items WHERE id = ?";
                    try (PreparedStatement deleteStatement = conn.prepareStatement(deleteQuery)) {
                        deleteStatement.setInt(1, maxId);
                        deleteStatement.executeUpdate();
                    }
                }
            }
        } finally {
            // Close the connection
            conn.close();
        }
    }



    // Method to total up the price for the specific order
    public static float getTotalPrice(int orderID) throws SQLException {
    	conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        PreparedStatement ps = conn.prepareStatement("SELECT SUM(price) as total_price FROM order_items WHERE order_id = ?");
        
        ps.setInt(1, orderID);
        ResultSet rs = ps.executeQuery();

        // Check if there is at least one row in the result set
        if (rs.next()) {
            // Retrieve data only if there is a row
            float price = rs.getFloat("total_price");
            ps.close();
            conn.close();
            return price;
        } else {
            // Handle the case when no rows are found
            ps.close();
            conn.close();
            return 0;  // or throw an exception or handle it based on your requirements
        }
    }

    /*
     * ----------------------------------- Product Operations--------------------------------------------------
     */

    // method to add the product into the database
    public static void addProduct(String name, Float price) throws SQLException {
        String query = "INSERT INTO products (name, price) VALUES (?, ?)";
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, name);
        stmt.setFloat(2, price);

        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    // Method to get product details from the database
    public static String[] getProd(int id) throws SQLException {
    	conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?");
        
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        // Check if there is at least one row in the result set
        if (rs.next()) {
            // Retrieve data only if there is a row
            String[] product = {rs.getString("id"), rs.getString("name"), rs.getString("price")};
            ps.close();
            conn.close();
            return product;
        } else {
            // Handle the case when no rows are found
            ps.close();
            conn.close();
            return null;  // or throw an exception or handle it based on your requirements
        }
    }

    /*
     * ----------------------------------- Customer Operations--------------------------------------------------
     */
    public static void addCustomer(String name, String phone, String email, String address) throws SQLException {
        String query = "INSERT INTO customers (name, phone, email, address) VALUES (?, ?, ?, ?)";
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, name);
        stmt.setString(2, phone);
        stmt.setString(3, email);
        stmt.setString(4, address);

        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    public static void delete(int id, String table) throws SQLException {
        String query = "DELETE FROM " + table + " WHERE id = ? ";
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, id);
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    // Method to update the comboBoxes with data from the database
    public static void updateCombox(String table, JComboBox<String> cbx) throws SQLException {
        cbx.removeAll();
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189");
        String sql = "SELECT * FROM " + table + ";";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            cbx.addItem(rs.getString("id") + "-|-" + rs.getString("name"));
        }

        rs.close();
        ps.close();
        conn.close();
    }

 // Method to Load Data from the database into the table
    public static void loadData(DefaultTableModel model, String table) throws SQLException {
        model.setRowCount(0);
        
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Supermarket", "root", "Y@mini81189")) {
            String sql = "SELECT * FROM " + table + ";";
            
            try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Object[] row = new Object[columnCount];
                    
                    for (int i = 0; i < columnCount; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    
                    model.addRow(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Rethrow the exception to handle it in the calling code
        }
    }

}
