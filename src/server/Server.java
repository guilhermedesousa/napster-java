package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    private static final int RMI_PORT = 1099;
    private static final String RMI_BINDING_NAME = "server";

    public static void main(String[] args) {
        try {
            ServerInterface si = new ServerImpl();

            Registry registry = LocateRegistry.createRegistry(RMI_PORT);

            registry.bind(RMI_BINDING_NAME, si);

            System.out.println("Server running...");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}
