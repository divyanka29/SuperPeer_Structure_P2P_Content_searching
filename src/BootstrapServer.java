import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class BootstrapServer {

	public static InetAddress RegistryIPAddr;
	public static Integer RegistryServSockPort;

	ArrayList<Node> nodes = new ArrayList<Node>();
	Random random;

	public String processRequest(BootstrapServer bsServer, String rcvdPacket)
	{
		System.out.println(rcvdPacket);
		String msg[] = rcvdPacket.split(" ");
		//System.out.println(msg[1]);
		if(msg[1].compareToIgnoreCase("REG")==0)
		{
			Node node = new Node(msg[2], Integer.parseInt(msg[3]));
			bsServer.nodes.add(node);
			//System.out.println(bsServer.nodes.size());
			if(bsServer.nodes.size()==1)
			{
				//length REGOK no_nodes IP_1 port_1 IP_2 port_2
				String regOKmsg = " REGOK 0";
				Integer lenMsg = regOKmsg.length() + 4;
				String len = "0000" + lenMsg.toString();
				String len1 = len.substring(len.length() - 4, len.length());
				regOKmsg = len1 + regOKmsg;
				//System.out.println(regOKmsg);
				return regOKmsg;
			}
			else if(bsServer.nodes.size()==2)
			{
				String regOKmsg = " REGOK 1 " + bsServer.nodes.get(0).nodeIP + " " + bsServer.nodes.get(0).nodePort;
				Integer lenMsg = regOKmsg.length() + 4;
				String len = "0000" + lenMsg.toString();
				String len1 = len.substring(len.length() - 4, len.length() - 1);
				regOKmsg = len1 + regOKmsg;
				//System.out.println(regOKmsg);
				return regOKmsg;
			}
			else if(bsServer.nodes.size()==3)
			{
				String regOKmsg = " REGOK 2 " + bsServer.nodes.get(0).nodeIP + " " + bsServer.nodes.get(0).nodePort + " " + bsServer.nodes.get(1).nodeIP + " " + bsServer.nodes.get(1).nodePort;
				Integer lenMsg = regOKmsg.length() + 4;
				String len = "0000" + lenMsg.toString();
				String len1 = len.substring(len.length() - 4, len.length());
				regOKmsg = len1 + regOKmsg;
				//System.out.println(regOKmsg);
				return regOKmsg;
			}
			else
			{
				random = new Random();
				int num1 = random.nextInt(bsServer.nodes.size()-1);
				int num2 = random.nextInt(bsServer.nodes.size()-1);
				while(num1==num2)
				{
					num2 = random.nextInt(bsServer.nodes.size()-1);
				}
				String regOKmsg = " REGOK 2 " + bsServer.nodes.get(num1).nodeIP + " " + bsServer.nodes.get(num1).nodePort + " " + bsServer.nodes.get(num2).nodeIP + " " + bsServer.nodes.get(num2).nodePort;
				Integer lenMsg = regOKmsg.length() + 4;
				String len = "0000" + lenMsg.toString();
				String len1 = len.substring(len.length() - 4, len.length() - 1);
				regOKmsg = len1 + regOKmsg;
				//System.out.println(regOKmsg);
				return regOKmsg;
			}
		}
		else
		{
			//System.out.println(rcvdPacket);
			String msg1[] = rcvdPacket.split(" ");
			//System.out.println(msg1[1]);
			
			bsServer.nodes = AuxFunctions.removeNode(bsServer.nodes, msg1[2], Integer.parseInt(msg1[3]));
			//System.out.println(bsServer.nodes.size());
			
			String unregOKmsg = " UNROK 0";
			String unregmsg = AuxFunctions.getMsg(unregOKmsg);
			//System.out.println(unregmsg);
			return unregmsg;
		}		
	}
	
	public static void main(String[] args) throws SocketException {
		// TODO Auto-generated method stub
		if (args.length != 1)
			throw new IllegalArgumentException("Parameter: Registry-Port");

		try {
			Integer.parseInt(args[0]);
		} catch (NumberFormatException ex) {
			System.out.println("The argument passed is not of the correct Data-type.");
		}

		BootstrapServer bs = new BootstrapServer();

		// Create a ServerSocket to accept client connection requests
		ServerSocket servSock = null;
		try {
			// Open the ServerSocket at the Port Number passed in the argument.
			servSock = new ServerSocket(Integer.parseInt(args[0]), 20);
		} catch (BindException e) {
			System.out.println();
			System.out.println("The Port is already in use. Please specify a different Port Number.");
			System.out.println();
			System.exit(0);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		bs.RegistryServSockPort = servSock.getLocalPort();
		bs.RegistryIPAddr = servSock.getInetAddress();

		Calendar curTime;

		// opening a udp socket to receive connections
		DatagramSocket serverSocket = new DatagramSocket(bs.RegistryServSockPort);

		// the server must run forever, accepting connections clients
		while (true) {
			try {
				byte[] receiveData = new byte[5000];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String rcvdPacket = new String(receivePacket.getData());
				
				String response = bs.processRequest(bs, rcvdPacket);
				
				byte[] sendData;
				sendData = response.getBytes();
								
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(rcvdPacket.split(" ")[2]), Integer.parseInt(rcvdPacket.split(" ")[3]));
				DatagramSocket clientSocket = new DatagramSocket();				
				clientSocket.send(sendPacket);
				
				// length REG IP_address port_no username
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
