import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Node extends Thread {
    private final int port;
    private String address;
    private String value; // key:value
    private final ArrayList<String> nodes;
    private final ArrayList<String> visitedNodes;

    public Node(int port, String value, ArrayList<String> nodes) {
        this.port = port;
        this.value = value;
        this.nodes = new ArrayList<>();
        this.visitedNodes = new ArrayList<>();
        for (String node : nodes) {
            String[] gatewayArray = node.split(":");
            String gateway = gatewayArray[0];
            int gatewayPort = Integer.parseInt(gatewayArray[1]);
            try {
                Socket netSocket = new Socket(gateway, gatewayPort);
                PrintWriter out = new PrintWriter(netSocket.getOutputStream(), true);
                out.println("new-node " + netSocket.getLocalAddress().getHostAddress() + ":" + port);
                netSocket.close();
                this.nodes.add(netSocket.getInetAddress().getHostAddress() + ":" + netSocket.getPort());
            } catch (IOException e) {
                System.err.println("No connection with " + gateway + ".");
            }
        }
    }

    @Override
    public void run() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (!serverSocket.isClosed()) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException ignore) {
                break;
            }

            this.address = clientSocket.getLocalAddress().getHostAddress();
            PrintWriter out;
            Scanner in;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String inputLine = in.nextLine();
            if (inputLine.startsWith("visited_nodes")) {
                String[] input = inputLine.split(" ");
                visitedNodes.addAll(Arrays.asList(input).subList(1, input.length));
                inputLine = in.nextLine();
            }
            String[] input = inputLine.split(" ");
            switch (input[0]) {
                case "set-value" -> {
                    if(input.length != 2) {
                        out.println("ERROR");
                        break;
                    }
                    if(input[1].split(":").length != 2) {
                        out.println("ERROR");
                        break;
                    }
                    String val = setValue(input[1]);
                    out.println(val);
                }
                case "get-value" -> {
                    if(input.length != 2) {
                        out.println("ERROR");
                        break;
                    }
                    String val = getValue(input[1]);
                    out.println(val);
                }
                case "find-key" -> {
                    if(input.length != 2) {
                        out.println("ERROR");
                        break;
                    }
                    String val = findKey(input[1]);
                    out.println(val);
                }
                case "get-max" -> {
                    String val = getMax();
                    out.println(val);
                }
                case "get-min" -> {
                    String val = getMin();
                    out.println(val);
                }
                case "new-record" -> {
                    if(input.length != 2) {
                        out.println("ERROR");
                        break;
                    }
                    if(input[1].split(":").length != 2) {
                        out.println("ERROR");
                        break;
                    }
                    String val = newRecord(input[1]);
                    out.println(val);
                }
                case "new-node" -> nodes.add(input[1]);
                case "remove-node" -> {
                    String[] val = input[1].split(";");
                    nodes.remove(val[0]);
                    out.println("OK");
                }
                case "terminate" -> {
                    String val = terminate();
                    out.println(val);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                default -> out.println("ERROR");
            }
            try {
                clientSocket.close();
                out.close();
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public String setValue(String pair) {
        String[] keyValue = value.split(":");
        String[] valueArray = pair.split(":");
        if (keyValue[0].equals(valueArray[0])) {
            value = pair;
            this.visitedNodes.clear();
            return "OK";
        }
        return communicateWithNodes("set-value " + pair);
    }

    public String getValue(String key) {
        String[] keyValue = value.split(":");
        if (key.equals(keyValue[0])) {
            this.visitedNodes.clear();
            return keyValue[1];
        }
        return communicateWithNodes("get-value " + key);
    }

    public String findKey(String key) {
        String[] val = value.split(":");
        if (val[0].equals(key)) {
            this.visitedNodes.clear();
            return this.address + ":" + this.port;
        }
        return communicateWithNodes("find-key " + key);
    }

    public String getMax() {
        String response;
        PrintWriter out;
        Scanner in;
        String max = value;
        for (String address : nodes) {
            if (visitedNodes.contains(address)) {
                continue;
            }
            this.visitedNodes.add(this.address + ":" + this.port);
            try {
                String[] gatewayArray = address.split(":");
                Socket node = new Socket(gatewayArray[0], Integer.parseInt(gatewayArray[1]));
                out = new PrintWriter(node.getOutputStream(), true);
                in = new Scanner(new InputStreamReader(node.getInputStream()));
                String visited = String.join(" ", this.visitedNodes);
                out.println("visited_nodes " + visited);
                out.println("get-max");
                response = in.nextLine();
                node.close();
                if (Integer.parseInt(response.split(":")[1]) > Integer.parseInt(max.split(":")[1])) {
                    max = response;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.visitedNodes.clear();
        return max;
    }

    public String getMin() {
        String response;
        PrintWriter out;
        Scanner in;
        String min = value;
        for (String address : nodes) {
            if (this.visitedNodes.contains(address)) {
                continue;
            }
            this.visitedNodes.add(this.address + ":" + this.port);
            try {
                String[] gatewayArray = address.split(":");
                Socket node = new Socket(gatewayArray[0], Integer.parseInt(gatewayArray[1]));
                out = new PrintWriter(node.getOutputStream(), true);
                in = new Scanner(new InputStreamReader(node.getInputStream()));
                String visited = String.join(" ", this.visitedNodes);
                out.println("visited_nodes " + visited);
                out.println("get-min");
                response = in.nextLine();
                node.close();
                if (Integer.parseInt(response.split(":")[1]) < Integer.parseInt(min.split(":")[1])) {
                    min = response;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.visitedNodes.clear();
        return min;
    }

    public String newRecord(String record) {
        this.value = record;
        return "OK";
    }

    public String terminate() {
        communicateWithNodes("remove-node " + this.address + ":" + this.port);
        return "OK";
    }

    private String communicateWithNodes(String command) {
        String response;
        PrintWriter out;
        Scanner in;
        for (String address : nodes) {
            if (this.visitedNodes.contains(address)) {
                continue;
            }
            this.visitedNodes.add(this.address + ":" + this.port);
            try {
                String[] gatewayArray = address.split(":");
                Socket node = new Socket(gatewayArray[0], Integer.parseInt(gatewayArray[1]));
                out = new PrintWriter(node.getOutputStream(), true);
                in = new Scanner(new InputStreamReader(node.getInputStream()));
                String visited = String.join(" ", this.visitedNodes);
                out.println("visited_nodes " + visited);
                out.println(command);
                response = in.nextLine();
                node.close();
                if (!response.equals("ERROR") && !command.startsWith("remove-node")) {
                    this.visitedNodes.clear();
                    return response;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.visitedNodes.clear();
        return "ERROR";
    }
}
