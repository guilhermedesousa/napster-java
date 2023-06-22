package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) {
        try {
            ServerInterface si = new ServerImpl();

            if (args.length < 2) {
                System.out.println("Usage: java server.Server <IP> <RMI_port>");
                return;
            }

            String ip = args[0];
            String port = args[1];

            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(port));

            registry.bind(String.format("rmi://%s/server", ip), si);

            System.out.println("Server running...");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}
