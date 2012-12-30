package vc.min.ryanshaw.Query;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class Query extends Thread{
	private static String host;
	private static int port;
	private static HashMap<String, String> values = new HashMap<String, String>();
	private String playersString = "";
	public int status = -1;
	
	public Query(String host, int port){
		this.host = host;	//Ignore how these are accessed.
		this.port = port;
	}
	
	public boolean startQuery(){
		
		try {
			status = 1;
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), 1000);
		} catch (Exception e){
			status = 2;
		}
		
		
		if(status == 1){
			this.start();
			return true;
			
		}
		else
			return false;
	}
	
	/*public static void main(String[] args){
		host = "minecraft.clanrks.com";
		port = 25565;
		new Query().start();
	}*/
	
	public String getValue(String key){
		if(values.containsKey(key))
			return values.get(key);
		else
			return "not found";
		
	}
	
	public String getPlayers(){
		return playersString;
		
	}
	
	@Override
	public void run(){
		playersString = "";
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e1) {
		}
		try {
			InetAddress ipAddress = InetAddress.getByName(host);
			byte[] sendData = new byte[10240]; // These seem like inefficient memory usage :(
			byte[] receiveData = new byte[10240];
			sendData = new byte[]{(byte)0xFE, (byte)0xFD, 0x09, 0x01, 0x01, 0x01, 0x01}; // Create handshake packet
			socket.setSoTimeout(2000); // Socket timeout 2000 millisecs
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
			socket.send(sendPacket); // Send handshake
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(receivePacket); // Receive reply

			byte[] preData = receivePacket.getData(); // Pre-processed data
			int byte1 = -1;
			int i = 0;
			byte[] buffer = new byte[8]; // For challenge token
			
			for(int count = 5; (byte1 = preData[count++]) != 0;) // Add bytes to challenge token
				buffer[i++] = (byte) byte1;
			
			int challengeInt = Integer.parseInt(new String(buffer).trim()); // Convert byte array to integer
			
			ByteBuffer bBuff = ByteBuffer.allocate(4); // Convert challenge int to byte array
			bBuff.putInt(challengeInt);
			
			sendData = concat(new byte[]{(byte)0xFE, (byte)0xFD, 0x00, 0x01, 0x01, 0x01, 0x01},bBuff.array()); // Magic bytes with session ID appended with challenge token
			sendData = concat(sendData, new byte[]{0x00, 0x00, 0x00, 0x00}); // Append padding
			sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port); // Create send packet
			socket.send(sendPacket); // Send packet
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(receivePacket); // Receive packet
			
			boolean onKey = true; // Are we on the key or the value			
			
			ArrayList<Byte> key = new ArrayList<Byte>(); // Builds up the key bytes in each cell
			ArrayList<Byte> value = new ArrayList<Byte>(); // Builds up the value bytes in each cell
			int i1;
			for(i1 = 5; i1 < receivePacket.getLength(); i1++){
				byte b = receivePacket.getData()[i1];
				if(b == 0){
					if(!onKey){ // If byte is 0 and not on key build the K/V and add the K/V to hashmap
						byte[] bufferKey = new byte[key.size()];
						if(key.size() == 1)
							break;
						for(i = 0; i < key.size(); i++){
							bufferKey[i] = key.get(i);
						}
						byte[] bufferValue = new byte[value.size()];
						
						for(i = 0; i < value.size(); i++){
							bufferValue[i] = value.get(i);
						}
						values.put(new String(bufferKey).trim(), new String(bufferValue).trim());
						key.clear();
						value.clear();
					}
					onKey = !onKey;
				}
				if(!onKey) //Add byte to appropiate list
					value.add(b);
				else 
					key.add(b);
			}
			//for(String key1 : values.keySet()) // Debug values
			//	System.out.println(key1 + " : "+ values.get(key1));
			
			byte[] buffer1 = new byte[32]; // Bytes should be enough for minecraft usernames...
			int count2 = 0;
			ArrayList<String> players = new ArrayList<String>();
			
			for(int c1 = i1; c1 < receivePacket.getLength(); c1++){
				byte b = receivePacket.getData()[c1];
				if(b == 0){
					players.add(new String(buffer1).trim());
					players.remove((Object)""); // Increase blank names got added
					buffer1 = new byte[32];
					count2 = 0;
					continue;
				}else
				buffer1[count2++] = b;
			}
			 
			for(i = 0; i < players.size(); i++){ // Build player string
				playersString = new StringBuilder(playersString).append(players.get(i)).append(",").toString();
			}
			if(playersString.length() != 0)
				playersString = playersString.substring(0,playersString.length()-1); // Take away comma on end
		} catch (SocketTimeoutException e) {
			status = 2;
		} catch (IOException e) {
			status = 2;
		}
		status = 3;
	}
	/*
	 * Concatenate two byte arrays together
	 */
	private static byte[] concat(byte[] A, byte[] B) {
	   byte[] C= new byte[A.length+B.length];
	   System.arraycopy(A, 0, C, 0, A.length);
	   System.arraycopy(B, 0, C, A.length, B.length);

	   return C;
	}
}
