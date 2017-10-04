package clients;

import clients.customcontrols.CustomDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import tools.Statics;
import tools.Toolbox;

public class MainApplication extends Application {

	private SceneManager sManager;
	private CustomDialog retryDlg;

	@Override
	public void start(Stage primaryStage) throws Exception {

		sManager = new SceneManager(primaryStage);
		sManager.setHost(this);
		int c;
		while (true) {
			Statics.DIMS_SERVER_IP_ADDRESS = CustomDialog.showInputDialog("���� IP�ּҸ� Ȯ�����ּ���.", sManager.getStage());
			Statics.DIMS_FILE_SERVER_IP_ADDRESS = Statics.DIMS_SERVER_IP_ADDRESS;

			if (Statics.DIMS_SERVER_IP_ADDRESS == null) {
				System.exit(-1);
			}

			c = sManager.connectToServer();

			if (c == Statics.COMMIT) {
				restart(primaryStage);
				break;
			}

			switch (c) {
			case Statics.CONNECT_ERROR:
				retryDlg = CustomDialog.showMessageDialog("�������ӽ���, �ڵ��������� �Ͻ÷��� â�� �������� ������", primaryStage);
				break;
			}
		}

		if (c == Statics.CONNECT_ERROR) {
			new Retry(sManager).start();
		}
	}

	public void restart(Stage primaryStage) {
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent event) {
				System.exit(1);
			}
		});

		sManager.changeScene(Statics.LOGIN_WINDOW_FXML);

		primaryStage.setResizable(false);
		primaryStage.show();
	}

	class Retry extends Thread {
		SceneManager m;

		Retry(SceneManager m) {
			this.m = m;
		}

		@Override
		public void run() {
			System.out.println("���� ������ �õ��� �����մϴ�.");
			int cnt = 0;
			while (true) {
				System.out.println("������ �õ�Ƚ�� : " + cnt++);
				System.out.print("���                 : ");
				int c = m.connectToServer();
				if (c != Statics.COMMIT) {
					System.out.println("����..");
					synchronized (this) {
						try {
							this.wait(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else {
					System.out.println("����! ���α׷��� �����ϰ� �����ӽ����带 �����մϴ�.");
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							restart(sManager.getStage());
							retryDlg.close();
						}
					});
					break;
				}

			}
		}
	}

}
