package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote {
    public String join(String ip, String port, List<String> fileNames) throws RemoteException;
    public List<String[]> search(String ip, String port, String fileName) throws RemoteException;
    public String update(String ip, String port, String fileName) throws RemoteException;
}
