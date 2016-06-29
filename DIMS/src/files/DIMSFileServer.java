package files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.Executor;

import com.orsoncharts.util.json.JSONObject;

import databases.DatabaseHandler;
import servers.controllers.ServerMainController;
import tools.Statics;
import tools.Toolbox;

public class DIMSFileServer {
	
	private ServerSocket fileSender;
	private boolean isTaskFinish = true;
	ServerMainController con;
	public DIMSFileServer(int port, ServerMainController con)
	{
		this.con = con;
		try
		{
			fileSender = new ServerSocket(port);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void serverClose()
	{
		try {
			fileSender.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void handleFileClient(JSONObject sendData, String userName)
	{
		new Thread(()->{
			
			Socket c = null;
			try
			{
				System.out.println("���� ��û ����, Ŭ���̾�Ʈ ���� �����");
				con.Log("F", "Waiting request clinet...");
				c = fileSender.accept();
				System.out.println("Ŭ���̾�Ʈ ����, ���� ������ ����");
				System.out.println(userName+"���� "+sendData.toJSONString());
				con.Log("F", "Connected. Start transmit thread.");
				new Connection(c, sendData).transmitFile();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}).start();
	}
	
	public void handleUploadClient(JSONObject info, DatabaseHandler handler, Runnable r)
	{
		byte[] recieve = new byte[(int)info.get("����ũ��")];
		new Thread(()->{
			
			Socket c = null;
			try
			{
				con.Log("F", "Waiting request client...");
				System.out.println("���� ��û ����, Ŭ���̾�Ʈ ���� �����");
				c = fileSender.accept();
				con.Log("F", "Connected. Start transmit thread.");
				System.out.println("Ŭ���̾�Ʈ ����, ���� ������ ����");
				
				ObjectInputStream is = new ObjectInputStream(c.getInputStream());
				for(int i=0;i<recieve.length;i++)
				{
					recieve[i] = (byte) is.read();
				}
				con.Log("F", "Recieve complete. length : "+recieve.length);				
				int vCnt = Toolbox.getResultSetSize(handler.excuteQuery("select * from �������"));
				
				String savePath = Statics.DEFAULT_MOVIE_DATA_DIRECTORY+info.get("�����̸�");
				
				handler.excuteUpdate("insert into �������(�����̸�,�������,����ũ��) values('"+info.get("�̸�")+"','"+savePath+"',"+recieve.length+");");
				Files.write(new File(savePath).toPath(), recieve);
				con.Log("F", "Save file at "+savePath);
				new Thread(r).start();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}			
		}).start();
	}
	
	class Connection
	{
		ObjectInputStream ois;
		ObjectOutputStream oos;
		Socket target;
		JSONObject sendData;
		public Connection(Socket target, JSONObject sendData)
		{
			this.target = target;
			this.sendData = sendData;
		}
		
		@SuppressWarnings("unchecked")
		public void transmitFile() 
		{
			System.out.println("���� Ŭ���̾�Ʈ ����");
			try
			{
				oos = new ObjectOutputStream(target.getOutputStream());
				
				ois = new ObjectInputStream(target.getInputStream());
				
				send(Toolbox.createJSONProtocol(FileProtocol.CONFIRM_READY));
				
				while(true)
				{
					JSONObject pack = (JSONObject)ois.readObject();
				
					if(pack==null)break;
					
					String type = pack.get("type").toString();
					
					if(type.equals(FileProtocol.READY_OK))
					{
						JSONObject protocol = Toolbox.createJSONProtocol(FileProtocol.DATA_INFO);
						
						protocol.put("fileName", sendData.get("fileName"));
						protocol.put("format", sendData.get("format"));
						
						send(protocol);
					}
					else if(type.equals(FileProtocol.REQUEST_CONTENT))
					{
						JSONObject protocol = Toolbox.createJSONProtocol(FileProtocol.CONTENT);
						send(protocol);
						con.Log("F", "Send data...");
						byte[] sData = (byte[])sendData.get("data");
						
						for(int i=0;i<sData.length;i++)
						{
							oos.writeByte(sData[i]);
						}
						oos.flush();
						con.Log("F", "Send complete. length : "+sData.length);
					}
					else if(type.equals(FileProtocol.BREAK_REQUEST))
					{
						send(Toolbox.createJSONProtocol(FileProtocol.BREAK_RESPOND));
					}
					
				}
			}
			catch(IOException|ClassNotFoundException e)
			{
				System.out.println("���� �۾� ����");
				con.Log("F", "Transmit task Finish");
				try {
					ois.close();
					oos.close();
				} catch (IOException e1)
				{
					e1.printStackTrace();
				}
				
			}
			finally
			{
				try {
					System.out.println("���� Ŭ���̾�Ʈ�� ��������");
					con.Log("F", "Disconnecting client...");
					ois.close();
					oos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		private void send(JSONObject pack)
		{
			try
			{
				oos.writeObject(pack);
				oos.flush();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}

	
	
}