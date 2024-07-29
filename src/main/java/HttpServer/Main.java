package HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MyRunnable implements Runnable{
    private final int port;
    private final ExecutorService executorService;
    public MyRunnable(final int port,final int concurrencyLevel){
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(concurrencyLevel);
    }
    @Override
    public void run() {
        Main m = new Main();
//        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            serverSocket.setReuseAddress(true);
//
//            executorService.submit(()->m.handleRequest(serverSocket));
//        }
//        catch (IOException ioException){
//            ioException.printStackTrace();
//        }
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started on port " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> m.handleRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }
}
public class Main {
    public void handleRequest(Socket socket) {
        BufferedReader in = null;
        PrintWriter out = null;
        FileInputStream fis = null;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String requestLine = in.readLine();
                System.out.println("Received request: " + requestLine);
                if (requestLine != null) {
                    String[] requestParts = requestLine.split(" ");
                    String path = requestParts[1];
                    if (path.equals("/")) {
                        out.println("HTTP/1.1 200 OK\r\n\r\n");
                    } else if (path.startsWith("/user-agent")) {
                        String userAgent = "";
                        String line;
                        while ((line = in.readLine()) != null && !line.isEmpty()) {
                            if (line.toLowerCase().startsWith("user-agent:")) {
                                userAgent = line.substring("user-agent:".length()).trim();
                                break;
                            }
                        }
                            System.out.println("The line is "+line);
                        String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + userAgent.length() + "\r\n\r\n" +
                                userAgent;
                        out.print(response);
                        out.flush();
                    } else if (path.startsWith("/echo")) {
                        String echoText = path.substring(6);
                        String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + echoText.length() + "\r\n\r\n" +
                                echoText;
                        out.print(response);
                        out.flush();
                    } else if (path.startsWith("/files")) {
                        String[] filePath = path.split("/");
                        System.out.println("The filepath is :"+filePath[2]);
                        System.out.println("The directory is :"+directory);
                        File f = new File(directory);
                        if (f.exists() && f.isDirectory()) {
                            File file = new File(f, filePath[2]);  // Changed from filePath[1] to filePath[2]
                            if (file.exists() && file.isFile()) {
                                byte[] bytes = new byte[(int) file.length()];
                                fis = new FileInputStream(file);
                                fis.read(bytes);
                                out.println("HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: application/octet-stream\r\n" +
                                        "Content-Length: " + bytes.length + "\r\n\r\n"+new String(bytes));
                                out.flush();
                                socket.getOutputStream().flush();
                            } else {
                                out.println("HTTP/1.1 404 Not Found\r\n\r\n");
                            }
                        } else {
                            out.println("HTTP/1.1 404 Not Found\r\n\r\n");
                        }
                    } else {
                        out.println("HTTP/1.1 404 Not Found\r\n\r\n");
                    }
                }
                System.out.println("Response sent.");

                // Break the loop after handling one request
                break;
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (fis != null) fis.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    static String directory = null;
    public static void main(String... args) throws IOException {
        for(int i =0; i< args.length;i++){
            if(args[i].startsWith("--d")){

                directory = args[++i];
            }
        }

        Thread t = new Thread(new MyRunnable(4221,10));
        t.start();

    }
}