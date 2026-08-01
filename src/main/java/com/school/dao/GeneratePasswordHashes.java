import org.mindrot.jbcrypt.BCrypt;

public class GeneratePasswordHashes {
    
    public static void main(String[] args) {
        String[] users = {"principal", "teacher1", "staff1"};
        String[] passwords = {"Admin@1234", "Teacher@1234", "Staff@1234"};

        System.out.println("=== BCrypt Hashes for Seed Data ===\n");

        for (int i = 0; i < users.length; i++) {
            String hash = BCrypt.hashpw(passwords[i], BCrypt.gensalt(12));
            System.out.println("Username : " + users[i]);
            System.out.println("Password : " + passwords[i]);
            System.out.println("Hash     : " + hash);
            System.out.println("--------------------------------------------------");
        }
    }
}