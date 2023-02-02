import java.util.ArrayList;

public class DatabaseNode {

    public static void main(String[] args) {
        int port = 8080;
        ArrayList<String> nodes = new ArrayList<>();
        String value = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-tcpport" -> port = Integer.parseInt(args[i + 1]);
                case "-record" -> value = args[i + 1];
                case "-connect" -> {nodes.add(args[i + 1]); i++;}
            }
        }
        new Node(port, value, nodes).start();
    }
}
