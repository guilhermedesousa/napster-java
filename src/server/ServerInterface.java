package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    public String join(String peerName) throws RemoteException;
}
