package gui;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JList;
import javax.swing.JButton;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observer;

import javax.swing.AbstractListModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTable;
import javax.swing.JScrollPane;

import system.Find;
import system.TransferMonitor;
import system.UserMonitor;
import tracker.TrackerMaintainerServer;

public class FileFlushGUI extends JFrame {
	private JPanel contentPane;
	private JTable transferTable;

	/**
	 * Create the frame.
	 */
	public FileFlushGUI(UserMonitor userMonitor, TransferMonitor transferMonitor) {
		setTitle("FileFlush");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 500);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnSettings = new JMenu("Settings");
		menuBar.add(mnSettings);
		
		JMenuItem mntmUsername = new JMenuItem("Username");
		mntmUsername.addActionListener(new SettingsUsernameDialog(userMonitor));
		mnSettings.add(mntmUsername);
		
		JMenuItem mntmDirectory = new JMenuItem("Directory");
		mntmDirectory.addActionListener(new SettingsDirectoryDialog(userMonitor));
		mnSettings.add(mntmDirectory);
		
		JMenuItem mntmManualUser = new JMenuItem("Add User");
		mntmManualUser.addActionListener(new SettingsManualUserDialog(userMonitor));
		mnSettings.add(mntmManualUser);
		
		JMenuItem mntmTracker = new JMenuItem("Tracker");
		mntmTracker.addActionListener(new SettingsTrackerDialog(userMonitor));
		mnSettings.add(mntmTracker);
		
		JMenuItem mntmMyTracker = new JMenuItem("Start Local Tracker");
		mntmMyTracker.addActionListener(new MyTrackerDialog(userMonitor));
		mnSettings.add(mntmMyTracker);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] {350, 200};
		gbl_contentPane.rowHeights = new int[] {200, 30, 50};
		gbl_contentPane.columnWeights = new double[]{0.0, 1.0};
		gbl_contentPane.rowWeights = new double[]{1.0, 1.0, 0.0};
		contentPane.setLayout(gbl_contentPane);
		
		UserJList userList = new UserJList(transferMonitor);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userList.setModel(new AbstractListModel() {
			String[] values = new String[] {};
			public int getSize() {
				return values.length;
			}
			public Object getElementAt(int index) {
				return values[index];
			}
		});
		userList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        userMonitor.addObserver((Observer) userList);
		GridBagConstraints gbc_userList = new GridBagConstraints();
		gbc_userList.gridheight = 2;
		gbc_userList.insets = new Insets(0, 0, 5, 5);
		gbc_userList.fill = GridBagConstraints.BOTH;
		gbc_userList.gridx = 0;
		gbc_userList.gridy = 0;
		contentPane.add(userList, gbc_userList);
		
		JButton btnFindUsers = new JButton("Find users");
		btnFindUsers.addActionListener(new FindButtonListener(userMonitor));
		
		GridBagConstraints gbc_btnFindUsers = new GridBagConstraints();
		gbc_btnFindUsers.insets = new Insets(0, 0, 0, 5);
		gbc_btnFindUsers.gridx = 0;
		gbc_btnFindUsers.gridy = 2;
		contentPane.add(btnFindUsers, gbc_btnFindUsers);
		
		JScrollPane transferScrollPane = new JScrollPane();
		GridBagConstraints gbc_transferScrollPane = new GridBagConstraints();
		gbc_transferScrollPane.gridheight = 3;
		gbc_transferScrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_transferScrollPane.fill = GridBagConstraints.BOTH;
		gbc_transferScrollPane.gridx = 1;
		gbc_transferScrollPane.gridy = 0;
		contentPane.add(transferScrollPane, gbc_transferScrollPane);
		
		transferTable = new TransferJTable();
		transferMonitor.addObserver((Observer) transferTable);
		transferTable.getColumnModel().getColumn(0).setPreferredWidth(150);
		transferTable.getColumnModel().getColumn(0).setMinWidth(150);
		transferTable.getColumnModel().getColumn(1).setPreferredWidth(250);
		transferTable.getColumnModel().getColumn(1).setMinWidth(200);
		transferScrollPane.setViewportView(transferTable);
	}

	private final class SettingsUsernameDialog implements ActionListener {
		private UserMonitor userMonitor;

		public SettingsUsernameDialog(UserMonitor userMonitor) {
			this.userMonitor = userMonitor;
		}

		public void actionPerformed(ActionEvent e) {
			String response = (String) JOptionPane.showInputDialog(null,
		            "Username",
		            "Change username",
		            JOptionPane.QUESTION_MESSAGE, null, null, userMonitor.getOwner().getUsername());
			if (response != null) {
				userMonitor.getOwner().setUsername(response.trim());
			}
		}
	}
	

	private final class SettingsManualUserDialog implements ActionListener {
		private UserMonitor userMonitor;

		public SettingsManualUserDialog(UserMonitor userMonitor) {
			this.userMonitor = userMonitor;
		}

		public void actionPerformed(ActionEvent e) {
			String response = (String) JOptionPane.showInputDialog(null,
		            "Address <address:port>",
		            "Manually add a user",
		            JOptionPane.QUESTION_MESSAGE, null, null, String.format("%s:%d", userMonitor.getOwner().getIPAddress(), userMonitor.getOwner().getPort()));
			if (response != null) {
				String[] data = response.split(":");
				try {
					userMonitor.addUser(new system.User(
							InetAddress.getByName(data[0].trim()),
							Integer.parseInt(data[1].trim()),
							"(Manually Added)")
					);
				} catch (NumberFormatException | UnknownHostException e1) {
					System.out.println("Failed to resolve manual user details");
					return;
				}
			}
		}
	}

	private final class SettingsTrackerDialog implements ActionListener {
		private UserMonitor userMonitor;

		public SettingsTrackerDialog(UserMonitor userMonitor) {
			this.userMonitor = userMonitor;
		}

		public void actionPerformed(ActionEvent e) {
			String response = (String) JOptionPane.showInputDialog(null,
		            "Address <address:port>",
		            "Change tracker address",
		            JOptionPane.QUESTION_MESSAGE, null, null, String.format("%s:%d", userMonitor.getTrackerHost(), userMonitor.getTrackerPort()));
			if (response != null) {
				String[] data = response.split(":");
				userMonitor.setTrackerAddress(data[0].trim(), Integer.parseInt(data[1].trim()));
			}
		}
	}
	
	private final class SettingsDirectoryDialog implements ActionListener {
		private UserMonitor userMonitor;

		public SettingsDirectoryDialog(UserMonitor userMonitor) {
			this.userMonitor = userMonitor;
		}

		public void actionPerformed(ActionEvent e) {
			String response = (String) JOptionPane.showInputDialog(null,
		            "Directory",
		            "Change download directory",
		            JOptionPane.QUESTION_MESSAGE, null, null, userMonitor.getDirectory().getAbsoluteFile());
			if (response != null) {
				File directory = new File(response.trim());
				if (!directory.isDirectory()) {
					int dialogResult = JOptionPane.showConfirmDialog(null,
									"The directory" + directory.getAbsolutePath() + " does not exist. Do you want to create it?",
									"Warning", JOptionPane.YES_NO_OPTION);
					if (dialogResult == JOptionPane.YES_OPTION){
						directory.mkdir();
						userMonitor.setDirectory(directory);
					}
				}
			}
		}
	}
	
	private static final class MyTrackerDialog implements ActionListener {
		private UserMonitor userMonitor;
		private tracker.TrackerMaintainerServer trackingServer;

		public MyTrackerDialog(UserMonitor userMonitor) {
			this.userMonitor = userMonitor;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				if (this.trackingServer == null) {
					this.trackingServer = new TrackerMaintainerServer(this.userMonitor);
				}
				else{
					System.out.println("Tracker already on");
				}
			} catch (InterruptedException e1) {
				return;
			}
		}
	}
	
	private class FindButtonListener implements ActionListener {
		
		private UserMonitor um;

		public FindButtonListener(UserMonitor um){
			this.um = um;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Find find = new Find();
	        find.setUser(um.getOwner());
	        find.start();
		}
	}
}
