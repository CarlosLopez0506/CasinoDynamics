package Utils;

import java.net.*;
import java.util.Enumeration;

public class NetworkUtils {

    /**
     * Obtiene la dirección IP basada en el parámetro `useLocal` (0 para dirección local 127.0.0.1,
     * 1 para dirección externa).
     *
     * @param useLocal Si es 0, devuelve la dirección local 127.0.0.1; si es 1, busca una dirección externa.
     * @return Dirección InetAddress asociada.
     * @throws SocketException Si ocurre un error al acceder a las interfaces de red.
     */
    public static InetAddress getAddress(int useLocal) throws SocketException, UnknownHostException {
        if (useLocal == 0) {
            // Si el parámetro es 0, devuelve la dirección local 127.0.0.1
            return InetAddress.getByName("127.0.0.1");
        } else if (useLocal == 1) {
            // Si el parámetro es 1, busca la primera dirección IPv4 válida externa
            return getExternalAddress();
        } else {
            throw new IllegalArgumentException("El parámetro useLocal debe ser 0 o 1.");
        }
    }

    // Método para obtener la primera dirección IPv4 válida externa
    private static InetAddress getExternalAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        System.out.println("Buscando la primera IPv4 válida...");

        // Si no se encuentra una interfaz Wi-Fi, devuelve la primera IPv4 válida
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        System.out.println("Primera dirección IPv4 válida encontrada: " + address.getHostAddress() + " en la interfaz " + networkInterface.getName());
                        return address;
                    }
                }
            }
        }

        System.out.println("No se encontraron direcciones IPv4 en ninguna interfaz activa.");
        throw new RuntimeException("No network interface found.");
    }
}
