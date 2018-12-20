/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *                                                                                             
 * Copyright (c) 2016-2018 The ZEN Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.vaklinov.zcashui.msg;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.cabecinha84.zelcashui.ZelCashJButton;
import com.cabecinha84.zelcashui.ZelCashJCheckBox;
import com.cabecinha84.zelcashui.ZelCashJFileChooser;
import com.cabecinha84.zelcashui.ZelCashJFrame;
import com.cabecinha84.zelcashui.ZelCashJLabel;
import com.cabecinha84.zelcashui.ZelCashJPanel;
import com.cabecinha84.zelcashui.ZelCashJProgressBar;
import com.cabecinha84.zelcashui.ZelCashJScrollPane;
import com.cabecinha84.zelcashui.ZelCashJSplitPane;
import com.cabecinha84.zelcashui.ZelCashJTabbedPane;
import com.cabecinha84.zelcashui.ZelCashJTextArea;
import com.cabecinha84.zelcashui.ZelCashJTextPane;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import com.vaklinov.zcashui.DataGatheringThread;
import com.vaklinov.zcashui.LabelStorage;
import com.vaklinov.zcashui.LanguageUtil;
import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;
import com.vaklinov.zcashui.SendCashPanel;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.Util;
import com.vaklinov.zcashui.WalletTabPanel;
import com.vaklinov.zcashui.ZCashClientCaller;
import com.vaklinov.zcashui.ZCashClientCaller.NetworkAndBlockchainInfo;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;
import com.vaklinov.zcashui.msg.Message.DIRECTION_TYPE;
import com.vaklinov.zcashui.msg.Message.VERIFICATION_TYPE;


/**
 * Main panel for messaging
 */
public class MessagingPanel
	extends WalletTabPanel
{
	private ZelCashJFrame parentFrame;
	private SendCashPanel sendCashPanel; 
	private ZelCashJTabbedPane parentTabs;
	
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	
	private MessagingStorage messagingStorage;
	
	private JContactListPanel contactList;
	
	private ZelCashJLabel conversationLabel;
	private ZelCashJTextPane conversationTextPane;
	
	private ZelCashJTextArea    writeMessageTextArea;
	private ZelCashJButton      sendButton;
	private ZelCashJLabel       sendResultLabel;
	private ZelCashJProgressBar sendMessageProgressBar;
	private ZelCashJCheckBox    sendAnonymously;
	
	private Timer operationStatusTimer = null;
	
	private DataGatheringThread<Object> receivedMesagesGatheringThread = null;
	
	private Long lastTaddressCheckTime = null;
	
	private boolean identityZAddressValidityChecked = false;
	
	private Object messageCollectionMutex = new Object();
	
	private IPFSWrapper ipfs;
	
	// Storage of labels
	private LabelStorage labelStorage;
	
	private static LanguageUtil langUtil = LanguageUtil.instance();
	
	public MessagingPanel(ZelCashJFrame parentFrame, SendCashPanel sendCashPanel, ZelCashJTabbedPane parentTabs, 
			              ZCashClientCaller clientCaller, StatusUpdateErrorReporter errorReporter,
			              LabelStorage labelStorage)
		throws IOException, InterruptedException, WalletCallException
	{
		super();
		
		this.parentFrame      = parentFrame;
		this.sendCashPanel    = sendCashPanel;
		this.parentTabs       = parentTabs;
		this.labelStorage     = labelStorage;
		
		this.clientCaller     = clientCaller;
		this.errorReporter    = errorReporter;
		this.messagingStorage = new MessagingStorage();
		this.ipfs             = new IPFSWrapper(parentFrame);
		
		// Start building UI
		this.setLayout(new BorderLayout(0, 0));
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		final ZelCashJSplitPane textAndContactsPane = new ZelCashJSplitPane();
		this.add(textAndContactsPane, BorderLayout.CENTER);
		
		this.contactList = new JContactListPanel(
			this, this.parentFrame, this.messagingStorage, this.errorReporter);
		textAndContactsPane.setRightComponent(this.contactList);
		
		ZelCashJPanel conversationPanel = new ZelCashJPanel(new BorderLayout(0, 0));
		conversationPanel.add(
			new ZelCashJScrollPane(
				this.conversationTextPane = new ZelCashJTextPane(),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 
			BorderLayout.CENTER);
		this.conversationTextPane.setEditable(false);
		this.conversationTextPane.setContentType("text/html");
		this.conversationTextPane.addHyperlinkListener(new GroupLinkHandler());
		ZelCashJPanel upperPanel = new ZelCashJPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		upperPanel.add(this.conversationLabel = new ZelCashJLabel(langUtil.getString("messaging.panel.upper.panel")));
		upperPanel.add(new ZelCashJLabel(
    			"<html><span style=\"font-size:1.6em;font-style:italic;\">&nbsp;</span>"));
		upperPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		conversationPanel.add(upperPanel, BorderLayout.NORTH);		
		
		textAndContactsPane.setLeftComponent(conversationPanel);
		SwingUtilities.invokeLater(new Runnable() { 
			@Override
			public void run() {
				textAndContactsPane.setDividerLocation(590);			
				}
		});
		
		
		ZelCashJPanel writeAndSendPanel = new ZelCashJPanel(new BorderLayout(0, 0));
		this.add(writeAndSendPanel, BorderLayout.SOUTH);
		
		ZelCashJPanel writePanel = new ZelCashJPanel(new BorderLayout(0, 0));
		this.writeMessageTextArea = new ZelCashJTextArea(3, 50);
		this.writeMessageTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		this.writeMessageTextArea.setLineWrap(true);
		writePanel.add(
			new ZelCashJScrollPane(this.writeMessageTextArea,
					        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 
			BorderLayout.CENTER);
		ZelCashJLabel sendLabel = new ZelCashJLabel(langUtil.getString("messaging.panel.message.send"));
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		if (ownIdentity != null)
		{
			sendLabel.setText(langUtil.getString("messaging.panel.message.send.as", ownIdentity.getDiplayString()));
		}
		sendLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		writePanel.add(sendLabel, BorderLayout.NORTH);
		writePanel.add(new ZelCashJLabel(""), BorderLayout.EAST); // dummy
		writeAndSendPanel.add(writePanel, BorderLayout.CENTER);
		
		ZelCashJPanel sendPanel = new ZelCashJPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		ZelCashJPanel sendButtonPanel = new ZelCashJPanel();
		sendButtonPanel.setLayout(new BoxLayout(sendButtonPanel, BoxLayout.Y_AXIS));
		ZelCashJLabel filler = new ZelCashJLabel(" ");
		filler.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		sendButtonPanel.add(filler); // filler
		sendButton = new ZelCashJButton(langUtil.getString("messaging.panel.send.message"));
		ZelCashJPanel tempPanel = new ZelCashJPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendButton);
		sendButtonPanel.add(tempPanel);
		sendMessageProgressBar = new ZelCashJProgressBar(0, 200);
		sendMessageProgressBar.setPreferredSize(
			new Dimension(sendButton.getPreferredSize().width, 
					      sendMessageProgressBar.getPreferredSize().height * 2 / 3));
		tempPanel = new ZelCashJPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendMessageProgressBar);
		sendButtonPanel.add(tempPanel);
		sendResultLabel = new ZelCashJLabel(
				langUtil.getString("messaging.panel.send.status"));
		tempPanel = new ZelCashJPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendResultLabel);
		sendButtonPanel.add(tempPanel);
		
		tempPanel = new ZelCashJPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(this.sendAnonymously = 
			new ZelCashJCheckBox(langUtil.getString("messaging.panel.send.anonymously")));
		sendButtonPanel.add(tempPanel);
		
		sendPanel.add(sendButtonPanel);
		writeAndSendPanel.add(sendPanel, BorderLayout.EAST);
		
		// Attach logic
		sendButton.addActionListener(new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				MessagingPanel.this.sendMessageAndHandleErrors();
			}
		});
				
		// Start the thread to periodically gather messages
		this.receivedMesagesGatheringThread = new DataGatheringThread<Object>(
			new DataGatheringThread.DataGatherer<Object>() 
			{
				public String[][] gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					
					MessagingPanel.this.collectAndStoreNewReceivedMessagesAndHandleErrors();
					
					long end = System.currentTimeMillis();
					Log.info("Gathering of received messages done in " + (end - start) + "ms." );
						
					return null;
				}
			}, 
			this.errorReporter, 45 * 1000, true);
		this.threads.add(receivedMesagesGatheringThread);
	}
	
	
	// Handler for hyperlinks in case of group messaging
	private class GroupLinkHandler
		implements HyperlinkListener
	{
		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) 
		{
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
				try
				{
					handleURL(e.getURL());
				} catch (Exception ex)
				{
					MessagingPanel.this.errorReporter.reportError(ex, false);
				}
			}
		}
		
		public void handleURL(URL u)
			throws IOException, URISyntaxException, InterruptedException
		{
			String id = u.toString();
			
			// Special handling of IPFS URLs
			if (MessagingPanel.this.ipfs.isIPFSURL(id))
			{
				MessagingPanel.this.ipfs.followIPFSLink(u);
				return;
			}
			
			// Handle uer links
			if (id.startsWith("http://"))
			{
				id = id.substring("http://".length());
				boolean anonymous = id.startsWith("ANON_");
				boolean normal = id.startsWith("NORM_");
				id = id.substring(5);
				
				MessagingIdentity selectedContact = MessagingPanel.this.contactList.getSelectedContact();
				if (selectedContact == null)
				{
					return;
				}
				
				String messageStart;
				Map<String, MessagingIdentity> senders = MessagingPanel.this.getKnownSendersForGroup(selectedContact);
				if (senders.containsKey(id))
				{
					MessagingIdentity sender = senders.get(id);
			        messageStart = langUtil.getString("messaging.panel.message.start", sender.getDiplayString(),sender.getSenderidaddress());
				} else
				{
			        messageStart = anonymous? langUtil.getString("messaging.panel.message.start2", id) :
			        	langUtil.getString("messaging.panel.message.start3", id);
				}
				
		        int reply1 = JOptionPane.showOptionDialog(
		        	MessagingPanel.this.parentFrame, 
			        messageStart + "\n" + 
			        langUtil.getString("messaging.panel.message.spamming"), 
			        langUtil.getString("messaging.panel.message.spamming.ignore"), 
			        JOptionPane.YES_NO_OPTION,
			        JOptionPane.WARNING_MESSAGE, 
			        null, new String[] { langUtil.getString("messaging.panel.message.spamming.ignore.yes"), 
			        		langUtil.getString("messaging.panel.message.spamming.ignore.cancel")}, 
			        JOptionPane.NO_OPTION);
			        
			    if (reply1 == JOptionPane.NO_OPTION) 
			    {
			    	return;
			    }
			    
			    Log.info("Ignoring all messages sent by user id {0} for group conversation {1}", 
			    		 id, selectedContact.getDiplayString());
			    MessagingPanel.this.messagingStorage.addIgnoredSenderIdentityForGroup(id, selectedContact);
			    MessagingPanel.this.displayMessagesForContact(selectedContact);
			}
		}
	} // End private class GroupLinkHandler

	
	/**
	 * Loads all messages for a specific contact and displays them in the conversation text area.
	 * 
	 * @param contact
	 */
	public void displayMessagesForContact(MessagingIdentity contact)
		throws IOException
	{
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		List<Message> messages = this.messagingStorage.getAllMessagesForContact(contact);
		
		// Analyze the received messages to extract from them messaging identities (if there are any)
		// TODO: This could be cached to optimize performance
		Map<String, MessagingIdentity> knownSenders = this.getKnownSendersForGroup(contact);
		
		Date now = new Date();
		StringBuilder text = new StringBuilder();

		final SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final SimpleDateFormat shortFormat = new SimpleDateFormat("HH:mm:ss");
		
		message_loop:
		for (Message msg : messages)
		{
			// Skip messages sent to a group from ignored IDs.
			String mesageIDToCheck = msg.isAnonymous() ? msg.getThreadID() : msg.getFrom();
			if (contact.isGroup() && (msg.getDirection() == DIRECTION_TYPE.RECEIVED) &&
				this.messagingStorage.isSenderIdentityIgnoredForGroup(mesageIDToCheck, contact))
			{
				Log.warningOneTime("Ignoring message sent to group {1} due to user preference: {0}",
						           msg.toJSONObject(false).toString(), contact.getDiplayString());
				continue message_loop;
			}
			
			// Skip message if sent from own id to group
			if (contact.isGroup() && (!msg.isAnonymous()) && (msg.getDirection() == DIRECTION_TYPE.RECEIVED) && 
				msg.getFrom().equals(ownIdentity.getSenderidaddress()))
			{
				continue message_loop;
			}
			
			String color = msg.getDirection() == DIRECTION_TYPE.SENT ? "blue" : "red";

			String stamp = defaultFormat.format(msg.getTime()); // TODO: correct date further
			if (Math.abs(now.getTime() - msg.getTime().getTime()) < (24L * 3600 * 1000)) // 24 h
			{
				if (now.getDay() == msg.getTime().getDay())
				{
					stamp = shortFormat.format(msg.getTime());
				}
			}
			
			String preparedMessage = null;
			
			if (this.isZelCashIdentityMessage(msg.getMessage()))
			{
				MessagingIdentity msgID = new MessagingIdentity(
					Util.parseJsonObject(msg.getMessage()).get("zelcashmessagingidentity").asObject());
				
				preparedMessage = "<span style=\"color:green;\">" +
					langUtil.getString("messaging.panel.special.identity") +
					msgID.getDiplayString() +
					"</span>";
			} else
			{
				// Replace line end characters, for multi-line messages
				preparedMessage = Util.escapeHTMLValue(msg.getMessage());
				preparedMessage = preparedMessage.replace("\n", "<br/>");
				// Possibly replace IPFS links
				preparedMessage = this.ipfs.replaceIPFSHTMLLinks(preparedMessage);
			};
			
			text.append("<span style=\"color:" + color +";\">");
			if (!contact.isGroup())
			{
				text.append("<span style=\"font-weight:bold;font-size:1.5em;\">");
				text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? "\u21E8 " : "\u21E6 ");
				text.append("</span>");
			}
			text.append("(");
			text.append(stamp); 
			text.append(") ");
			
			if (!msg.isAnonymous())
			{
				if ((msg.getDirection() == DIRECTION_TYPE.RECEIVED) && 
					(msg.getVerification() == VERIFICATION_TYPE.UNVERIFIED))
				{
					text.append("<span style=\"font-weight:bold;\">");
					text.append(langUtil.getString("messaging.panel.warning.unverified"));
					text.append("</span>");	
				} else if ((msg.getDirection() == DIRECTION_TYPE.RECEIVED) && 
						   (msg.getVerification() == VERIFICATION_TYPE.VERIFICATION_FAILED))
				{
					text.append("<span style=\"font-weight:bold;font-size:1.25em;\">");
					text.append(langUtil.getString("messaging.panel.error.invalid"));
					text.append("</span>");
				}
			} else
			{
				text.append(contact.isGroup() && (msg.getDirection() == DIRECTION_TYPE.RECEIVED) ? 
					"<a href=\"http://ANON_" + msg.getThreadID() + "\">" : "");
				text.append("<span style=\"font-weight:bold;\">");
				text.append(langUtil.getString("messaging.panel.anonymous"));
				text.append(contact.isGroup() ? "[" + msg.getThreadID().substring(0, 15) + "...] " : "");
				text.append("</span>");
				text.append(contact.isGroup() && (msg.getDirection() == DIRECTION_TYPE.RECEIVED) ? "</a>" : "");
			}
			
			// Try to resolve the identity of the sender
			MessagingIdentity groupSenderIdentity = knownSenders.containsKey(msg.getFrom()) ?
				knownSenders.get(msg.getFrom()) : null;
			String groupSenderNickName = (groupSenderIdentity != null) ?
				groupSenderIdentity.getDiplayString() : ("<" + msg.getFrom() + ">");
			String senderNickname = contact.isGroup() ?
				Util.escapeHTMLValue(groupSenderNickName) :
		        Util.escapeHTMLValue(contact.getNickname());
			
			if ((!msg.isAnonymous()) || (msg.getDirection() == DIRECTION_TYPE.SENT))
			{
				text.append(contact.isGroup() && (msg.getDirection() == DIRECTION_TYPE.RECEIVED) ? 
						"<a href=\"http://NORM_" + msg.getFrom() + "\">" : "");
				text.append("<span style=\"font-weight:bold;\">");
				text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? 
						    Util.escapeHTMLValue(ownIdentity.getNickname()) : senderNickname);
				text.append("</span>");
				text.append(contact.isGroup() && (msg.getDirection() == DIRECTION_TYPE.RECEIVED) ? "</a>" : "");
			}
			text.append(": ");
			text.append("</span>");
			text.append(preparedMessage);
			text.append("<br/>");
		}
		
		this.conversationTextPane.setText("<html>" + text.toString() + "</html>");
		
		if (contact.isGroup())
		{
			this.conversationLabel.setText(
				"<html><span style=\"font-size:1.25em;font-style:italic;\">" + langUtil.getString("messaging.panel.conversation.group") + 
			    contact.getDiplayString() + "</span>");			
		} else
		{
			this.conversationLabel.setText(
				"<html><span style=\"font-size:1.25em;font-style:italic;\">" + langUtil.getString("messaging.panel.conversation.with") + 
		        contact.getDiplayString() + "</span>");
		}
	}
	

	/**
	 * Called when the TAB is selected - currently shows the welcome message
	 */
	public void tabSelected()
	{
		try
		{
			if (this.messagingStorage.getOwnIdentity() == null)
			{
				Object[] optionsSkip = 
		        	{ 
		        		langUtil.getString("button.option.ok"),
		        		langUtil.getString("button.option.skip")
		        	};
				int reply = JOptionPane.showOptionDialog(
	                this.parentFrame,
	                langUtil.getString("messaging.panel.welcome.message", OSUtil.getSettingsDirectory()+ File.separator +"messaging"),
	                langUtil.getString("messaging.panel.welcome"), 
			        JOptionPane.YES_NO_OPTION,
			        JOptionPane.QUESTION_MESSAGE,
			        null,
			        optionsSkip,
			        JOptionPane.NO_OPTION);
				
				if (reply == JOptionPane.NO_OPTION) 
		        {
		        	return;
		        }
		        	        
		        // Show the GUI dialog to edit an initially empty messaging identity
		        boolean identityCreated = this.openOwnIdentityDialog();
		        Object[] options = 
		        	{ 
		        		langUtil.getString("button.option.yes"),
		        		langUtil.getString("button.option.no")
		        	};
		        // Offer the user to export his messaging identity
		        reply = JOptionPane.showOptionDialog(
		        	this.parentFrame, 
		        	langUtil.getString("messaging.panel.export.message"), 
		        	langUtil.getString("messaging.panel.export"), 
			        JOptionPane.YES_NO_OPTION,
			        JOptionPane.QUESTION_MESSAGE,
			        null,
			        options,
			        JOptionPane.NO_OPTION);
		        
		        if (reply == JOptionPane.YES_OPTION) 
		        {
		        	this.exportOwnIdentity();
		        }
		        
				if (identityCreated)
				{
					MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
					
			        JOptionPane.showMessageDialog(
				        this.parentFrame,
				        langUtil.getString("messaging.panel.address.message", ownIdentity.getSendreceiveaddress() ), 
					    langUtil.getString("messaging.panel.address"), 
					    JOptionPane.INFORMATION_MESSAGE);
					        
						sendCashPanel.prepareForSending(ownIdentity.getSendreceiveaddress());
			            parentTabs.setSelectedIndex(3);				
				}
			} else
			{
				if ((this.lastTaddressCheckTime == null) ||
					((System.currentTimeMillis() - this.lastTaddressCheckTime) > (30 * 60 * 1000)))
				{
					this.lastTaddressCheckTime = System.currentTimeMillis();
				} else
				{
					return;
				}
				
				// Own identity exists, check balance of T address !!! - must be none
				MessagingIdentity ownIdentity =  this.messagingStorage.getOwnIdentity();
				Cursor oldCursor = this.parentFrame.getCursor();
				String balance = null;
				try
				{
     				this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		    	    balance = this.clientCaller.getBalanceForAddress(ownIdentity.getSenderidaddress());
				} finally
				{
					this.parentFrame.setCursor(oldCursor);
				}
				
		    	if (Double.valueOf(balance) > 0)
		    	{
			        JOptionPane.showMessageDialog(
					    this.parentFrame,
					    langUtil.getString("messaging.panel.identification.message", ownIdentity.getSenderidaddress()), 
						langUtil.getString("messaging.panel.identification"), 
						JOptionPane.WARNING_MESSAGE);
		    	}
			}
		} catch (Exception ex)
		{
			Log.error("Unexpected error in messaging TAB selection processing", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	public void openOptionsDialog()
	{
		try
		{
			MessagingOptionsEditDialog optionsDialog = new MessagingOptionsEditDialog(
				this.parentFrame, this.messagingStorage, this.errorReporter);
			optionsDialog.setVisible(true);
			
		} catch (Exception ex)
		{
			Log.error("Unexpected error in editing options!", ex);
			this.errorReporter.reportError(ex, false);
		}
		
	}
	
	
	/**
	 * Shows the UI dialog to edit+save one's own identity.
	 * 
	 * @return true if new identity was created.
	 */
	public boolean openOwnIdentityDialog()
	{
		try
		{
			MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
			boolean identityIsBeingCreated = false;
			
			if (ownIdentity == null)
			{
				identityIsBeingCreated = true;
				ownIdentity = new MessagingIdentity();
				
				Cursor oldCursor = this.getCursor();
				try
				{
     				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				     // Create the T/Z addresses to be used for messaging - sometimes the wallet returns old
     				 // T addresses, so we iterate
				     String TAddress = null;
				     for (int i = 0; i < 10; i++)
				     {
				    	 TAddress = this.clientCaller.createNewAddress(false, false);
				    	 String balance = this.clientCaller.getBalanceForAddress(TAddress);
				    	 if (Double.valueOf(balance) <= 0)
				    	 {
				    		 break;
				    	 }
				     }
				    // Generate sapling addresses for messages
				     String ZAddress = this.clientCaller.createNewAddress(true, true);
				     
					// Update the labels for the two addresses
				    this.labelStorage.setLabel(TAddress, langUtil.getString("messaging.panel.own.t"));
				    this.labelStorage.setLabel(ZAddress, langUtil.getString("messaging.panel.own.z"));
				     
					ownIdentity.setSenderidaddress(TAddress);
					ownIdentity.setSendreceiveaddress(ZAddress);
				} finally
				{
					this.setCursor(oldCursor);
				}				
			}
			
			// Dialog will automatically save the identity if the user chooses so 
			OwnIdentityEditDialog ownIdentityDialog = new OwnIdentityEditDialog(
				this.parentFrame, ownIdentity, this.messagingStorage, this.errorReporter, identityIsBeingCreated);
			ownIdentityDialog.setVisible(true);
			
			return identityIsBeingCreated;
			
		} catch (Exception ex)
		{
			Log.error("Unexpected error in editing own messaging identity!", ex);
			this.errorReporter.reportError(ex, false);
			
			return false;
		}
	}
	
	
	/**
	 * Exports a user's own identity to a file.
	 */
	public void exportOwnIdentity()
	{
		try
		{
			MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
			
			if (ownIdentity == null)
			{
		        JOptionPane.showMessageDialog(
	        		this.parentFrame,
	        		langUtil.getString("messaging.identity.missing.message"), 
	        		langUtil.getString("messaging.identity.missing"), JOptionPane.ERROR_MESSAGE);
		        return;
			}
			
			String nick = ownIdentity.getNickname();
			String filePrefix = "";
			
			for (char c : nick.toCharArray())
			{
				if (Character.isJavaIdentifierStart(c) || Character.isDigit(c))
				{
					filePrefix += c;
				}
			}
			
			ZelCashJFileChooser fileChooser = new ZelCashJFileChooser();
			fileChooser.setDialogTitle(langUtil.getString("messaging.identity.filechooser.title"));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setSelectedFile(
				new File(OSUtil.getUserHomeDirectory(), filePrefix + "_messaging_identity.json"));
			 
			int result = fileChooser.showSaveDialog(this.parentFrame);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			JsonObject identityObject = new JsonObject();
			identityObject.set("zelcashmessagingidentity", ownIdentity.toJSONObject(true));
			String identityString = identityObject.toString(WriterConfig.PRETTY_PRINT);
			
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream(f);
				OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
				osw.write(identityString);
				osw.flush();
			} finally
			{
				if (fos != null)
				{
					fos.close();
				}
			}
			
			JOptionPane.showMessageDialog(
				this.parentFrame, 
				langUtil.getString("messaging.identity.export", f.getName()),
				langUtil.getString("messaging.identity.successfully.exported"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex)
		{
			Log.error("Unexpected error in exporting own messaging identity to file!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	/**
	 * Imports a contact's identity from file.
	 */
	public void importContactIdentity()
	{
		try
		{
			ZelCashJFileChooser fileChooser = new ZelCashJFileChooser();
			fileChooser.setDialogTitle("messaging.identity.import.title");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			 
			int result = fileChooser.showOpenDialog(this.parentFrame);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();

			JsonObject topIdentityObject = null;
			
			Reader r = null;
			try
			{
				r = new InputStreamReader(new FileInputStream(f), "UTF-8");
				topIdentityObject = Util.parseJsonObject(r);
			} finally
			{
				if (r != null)
				{
					r.close();
				}
			}
			
			// Validate the fields inside the objects, make sure this is indeed an identity
			// verify mandatory etc.
			JsonValue innerValue = topIdentityObject.get("zelcashmessagingidentity");
			JsonObject innerIdentity = (innerValue != null) ? innerValue.asObject() : null;
			
			if ((innerValue == null) || (innerIdentity == null) ||
				(innerIdentity.get("nickname") == null) ||
				(innerIdentity.get("sendreceiveaddress") == null) ||
				(innerIdentity.get("senderidaddress") == null))
			{
				JOptionPane.showMessageDialog(
					this.parentFrame, 
					langUtil.getString("messaging.identity.import.wrong.format"), 
					langUtil.getString("messaging.identity.import.format"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			MessagingIdentity contactIdentity = new MessagingIdentity(innerIdentity);
			
			// Search through the existing contact identities, to make sure we are not adding it a second time
			for (MessagingIdentity mi : this.messagingStorage.getContactIdentities(false))
			{
				if (mi.isIdenticalTo(contactIdentity))
				{  
					Object[] options = 
			        	{ 
			        		langUtil.getString("button.option.yes"),
			        		langUtil.getString("button.option.no")
			        	};
			        int choice = JOptionPane.showOptionDialog(
		        		this.parentFrame,
		        		langUtil.getString("messaging.identity.import.duplicate.user", mi.getDiplayString(), contactIdentity.getDiplayString()),
		        		langUtil.getString("messaging.identity.import.same.user"), JOptionPane.YES_NO_OPTION,
				        JOptionPane.QUESTION_MESSAGE,
				        null,
				        options,
				        JOptionPane.NO_OPTION);
			        
			        if (choice == JOptionPane.YES_OPTION) 
			        {
			        	this.messagingStorage.updateContactIdentityForSenderIDAddress(
			        		contactIdentity.getSenderidaddress(), contactIdentity);
						JOptionPane.showMessageDialog(
							this.parentFrame, 
							langUtil.getString("messaging.identity.import.same.user.update"), 
							langUtil.getString("messaging.identity.import.update"), JOptionPane.INFORMATION_MESSAGE);
						this.contactList.reloadMessagingIdentities();
			        }
			        
			        // In any case - not a new identity to add
			        return;
				}
			}
			
			// Check for the existence of an "Unknown" type of identity already - that could be
			// updated. Search can be done by T address only.
			MessagingIdentity existingUnknownID = 
				this.messagingStorage.getContactIdentityForSenderIDAddress(contactIdentity.getSenderidaddress());
			if (existingUnknownID != null)
			{
		        int choice = JOptionPane.showConfirmDialog(
		        	this.parentFrame,
		        	langUtil.getString("messaging.identity.import.same.sender",
		        			existingUnknownID.getDiplayString(),
		        			contactIdentity.getDiplayString()),
		        	langUtil.getString("messaging.identity.import.sender"), 
		        	JOptionPane.YES_NO_OPTION);
			        
			    if (choice == JOptionPane.YES_OPTION) 
			    {
			       	this.messagingStorage.updateContactIdentityForSenderIDAddress(
			       		contactIdentity.getSenderidaddress(), contactIdentity);
					JOptionPane.showMessageDialog(
						this.parentFrame, 
						langUtil.getString("messaging.identity.import.sender.update.message"), 
						langUtil.getString("messaging.identity.import.sender.update"), JOptionPane.INFORMATION_MESSAGE);
					this.contactList.reloadMessagingIdentities();
			    }
				
				return;
			}
		
			// Add the new identity normally!
			this.messagingStorage.addContactIdentity(contactIdentity);
			Object[] options = 
	        	{ 
	        		langUtil.getString("button.option.yes"),
	        		langUtil.getString("button.option.no")
	        	};
			int sendIDChoice = JOptionPane.showOptionDialog(
				this.parentFrame, 
				langUtil.getString("messaging.identity.import.successfully", 
						contactIdentity.getDiplayString()),
				langUtil.getString("messaging.identity.import.send.identity"), JOptionPane.YES_NO_OPTION,
		        JOptionPane.QUESTION_MESSAGE,
		        null,
		        options,
		        JOptionPane.NO_OPTION);
			
			this.contactList.reloadMessagingIdentities();
			
			if (sendIDChoice == JOptionPane.YES_OPTION)
			{
				this.sendIdentityMessageTo(contactIdentity);
			}
			
		} catch (Exception ex)
		{
			Log.error("Unexpected error in importing contact messaging identity from file!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	/**
	 * GUI initiated removal
	 */
	public void removeSelectedContact()
	{
		try
		{
			// Make sure contacts are available
			if (this.contactList.getNumberOfContacts() <= 0)
			{
		        JOptionPane.showMessageDialog(
	        		this.parentFrame,
	        		langUtil.getString("messaging.identity.remove.selected.contact"),
		        	langUtil.getString("messaging.identity.remove.no.contacts"), JOptionPane.ERROR_MESSAGE);					
				return;			
			}
			
			MessagingIdentity id = this.contactList.getSelectedContact();
			
			if (id == null)
			{
		        JOptionPane.showMessageDialog(
		        	this.parentFrame,
		        	langUtil.getString("messaging.identity.remove.none.selected"),
			        langUtil.getString("messaging.identity.remove.not.selected"), JOptionPane.ERROR_MESSAGE);					
				return;
			}
			
	        // Offer the user a final warning on removing the contact
			String contactTAddress = Util.stringIsEmpty(id.getSenderidaddress()) ? 
					                 "<NONE>" : id.getSenderidaddress();
			String contactZAddress = Util.stringIsEmpty(id.getSendreceiveaddress()) ? 
	                                 "<NONE>" : id.getSendreceiveaddress();	
			Object[] options = 
	        	{ 
	        		langUtil.getString("button.option.yes"),
	        		langUtil.getString("button.option.no")
	        	};
			
	        int reply = JOptionPane.showOptionDialog(
	        	this.parentFrame,
	        	id.isGroup() ? langUtil.getString("messaging.identity.remove.group.warning", 
	        			id.getDiplayString(),
	        			contactTAddress,
	        			contactZAddress) : langUtil.getString("messaging.identity.remove.contact.warning", 
	    	        			id.getDiplayString(),
	    	        			contactTAddress,
	    	        			contactZAddress),
	        	langUtil.getString("messaging.identity.remove.warning"), 
	        	JOptionPane.YES_NO_OPTION,
		        JOptionPane.QUESTION_MESSAGE,
		        null,
		        options,
		        JOptionPane.NO_OPTION);
	        
	        if (reply == JOptionPane.NO_OPTION) 
	        {
	        	return;
	        }

	        Cursor oldCursor = this.parentFrame.getCursor();
	        try
	        {
	        	this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        	synchronized (this.messageCollectionMutex)
	        	{
	        		this.messagingStorage.deleteContact(id);
	        		this.messagingStorage.addIgnoredContact(id);
	        		
					MessagingPanel.this.contactList.reloadMessagingIdentities();
					
					// Reload the messages for the currently selected user - in the AWT event thread in the mutex!
					final MessagingIdentity selectedContact = MessagingPanel.this.contactList.getSelectedContact();
					if (selectedContact != null)
					{
						MessagingPanel.this.displayMessagesForContact(selectedContact);
					}
	        	}
	        } finally
	        {
	        	this.parentFrame.setCursor(oldCursor);
	        }			
		} catch (Exception ex)
		{
			Log.error("Unexpected error in removing contact!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}

	
	private void sendMessageAndHandleErrors()
	{
		try
		{
			sendMessage(null, null);
		} catch (Exception e)
		{
			Log.error("Unexpected error in sending message (wrapper): ", e);
			this.errorReporter.reportError(e);
		}
	}
	
	
	// String textToSend - if null, taken from the text area
	// MessagingIdentity remoteIdentity - if null selection is taken
	private void sendMessage(String textToSend, MessagingIdentity remoteIdentity)
		throws IOException, WalletCallException, InterruptedException
	{
		boolean sendAnonymously = this.sendAnonymously.isSelected();
		boolean sendReturnAddress = false;
		boolean updateMessagingIdentityJustBeforeSend = false;
				
		// Make sure contacts are available
		if (this.contactList.getNumberOfContacts() <= 0)
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		langUtil.getString("messaging.identity.remove.selected.contact"),
	        	langUtil.getString("messaging.identity.remove.no.contacts"), JOptionPane.ERROR_MESSAGE);					
			return;			
		}

		if ((remoteIdentity == null) && (this.contactList.getSelectedContact() == null))
		{
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	langUtil.getString("messaging.send.none.selected"),
		        langUtil.getString("messaging.identity.remove.not.selected"), JOptionPane.ERROR_MESSAGE);					
			return;		
		}
		
		// Create a copy of the identity to make sure changes made temporarily to do get reflected until
		// storage s updated (such a change may be setting a Z address)
		final MessagingIdentity contactIdentity = 
			(remoteIdentity != null) ? remoteIdentity.getCloneCopy() : 
				                       this.contactList.getSelectedContact().getCloneCopy();
			
		// Make sure contact identity is full (not Unknown with no address to send to)
		if (Util.stringIsEmpty(contactIdentity.getSendreceiveaddress()))
		{
			String errroMessage = 
					langUtil.getString("messaging.send.error.not.valid", contactIdentity.getDiplayString());
		
			errroMessage += contactIdentity.isAnonymous() ?
				(langUtil.getString("messaging.send.error.anonymou"))
				:
				(langUtil.getString("messaging.send.error.not.anonymous"));
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		errroMessage,
	        	langUtil.getString("messaging.send.error.need.z"), JOptionPane.ERROR_MESSAGE);					
			return;
		}
		
		// If the message is being sent anonymously, make sure there is already a thread ID
		// set for the recipient. Also ask the user if he wishes to send a return address.
		if (sendAnonymously)
		{
			// If also no thread ID is set yet...
			if (Util.stringIsEmpty(contactIdentity.getThreadID()))
			{
				if (!contactIdentity.isGroup())
				{
					Object[] options = 
			        	{ 
			        		langUtil.getString("button.option.yes"),
			        		langUtil.getString("button.option.no")
			        	};
			        // Offer the user to send a return address
			        int reply = JOptionPane.showOptionDialog(
			        	this.parentFrame, 
			        	langUtil.getString("messaging.send.first.message", contactIdentity.getDiplayString()),
			        	langUtil.getString("messaging.send.return.address"), 
			        	JOptionPane.YES_NO_OPTION,
				        JOptionPane.QUESTION_MESSAGE,
				        null,
				        options,
				        JOptionPane.NO_OPTION);
			        
			        if (reply == JOptionPane.YES_OPTION) 
			        {
			        	sendReturnAddress = true;
			        }
				}

		        String threadID = UUID.randomUUID().toString();
		        contactIdentity.setThreadID(threadID);
		        // If there is no thread ID, this must be a "normal" identity. An anonymous one
		        // will have a thread ID set on the first arriving message!
		        if (contactIdentity.isGroup() || (!Util.stringIsEmpty(contactIdentity.getSenderidaddress())))
		        {
		        	updateMessagingIdentityJustBeforeSend = true;
		        } else
		        {
			        JOptionPane.showMessageDialog(
		        		this.parentFrame,
		        		langUtil.getString("messaging.send.error.need.t", contactIdentity.getDiplayString()), 
			        	langUtil.getString("messaging.send.error.t"), JOptionPane.ERROR_MESSAGE);					
					return;
		        }
			}
		} else
		{
			// Check to make sure a normal message is not being sent to an anonymous identity
			if (contactIdentity.isAnonymous())
			{
				Object[] options = 
		        	{ 
		        		langUtil.getString("button.option.yes"),
		        		langUtil.getString("button.option.no")
		        	};
		        int reply = JOptionPane.showOptionDialog(
			      	this.parentFrame, 
			      	langUtil.getString("messaging.send.reveal.t.message", contactIdentity.getDiplayString()),
			       	langUtil.getString("messaging.send.reveal.t"), 
			       	JOptionPane.YES_NO_OPTION,
			        JOptionPane.QUESTION_MESSAGE,
			        null,
			        options,
			        JOptionPane.NO_OPTION);
			        
			    if (reply == JOptionPane.NO_OPTION) 
			    {
			       	return;
			    }
			}
		}
		
		// Get the text to send as a message
		if (textToSend == null)
		{
			textToSend = this.writeMessageTextArea.getText();
		}
		
		if (textToSend.length() <= 0)
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		langUtil.getString("messaging.send.no.text.message"), 
	        	langUtil.getString("messaging.send.no.text"), JOptionPane.ERROR_MESSAGE);					
			return;
		}
		
		// Make sure there is not another send operation going on - at this time
		if ((this.operationStatusTimer != null) || (!this.sendButton.isEnabled()))
		{
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	langUtil.getString("messaging.send.another.message"), 
		        langUtil.getString("messaging.send.another"), JOptionPane.ERROR_MESSAGE);					
			return;
		}
		
		// Disable sending controls, set status.
		this.sendButton.setEnabled(false);
		this.writeMessageTextArea.setEnabled(false);
				
		// Form the JSON message to be sent
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity(); 
		
		MessagingOptions msgOptions = this.messagingStorage.getMessagingOptions();
		
		// Check to make sure the sending address has some funds!!!
		final double minimumBalance = msgOptions.getAmountToSend() + msgOptions.getTransactionFee();
		
		Double balance = null;
		Double unconfirmedBalance = null;
		Cursor oldCursor = this.parentFrame.getCursor();
		try
		{
			this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			balance = Double.valueOf(
				this.clientCaller.getBalanceForAddress(ownIdentity.getSendreceiveaddress()));
			unconfirmedBalance = Double.valueOf(
				this.clientCaller.getUnconfirmedBalanceForAddress(ownIdentity.getSendreceiveaddress()));
		} finally
		{
			this.parentFrame.setCursor(oldCursor);
		}		
		
		if ((balance < minimumBalance) && (unconfirmedBalance < minimumBalance))
		{
			Log.warning("Sending address has balance: {0} and unconfirmed balance: {1}",
					    balance, unconfirmedBalance);
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	langUtil.getString("messaging.send.no.funds.message", ownIdentity.getSendreceiveaddress()),
		        langUtil.getString("messaging.send.no.funds"), JOptionPane.ERROR_MESSAGE);
		        
	            // Restore controls and move to the send cash tab etc.
		        this.sendButton.setEnabled(true);
				this.writeMessageTextArea.setEnabled(true);
				
				sendCashPanel.prepareForSending(ownIdentity.getSendreceiveaddress());
	            parentTabs.setSelectedIndex(3);				
				return;
		}
		
		if ((balance < minimumBalance) && (unconfirmedBalance >= minimumBalance))
		{
			Log.warning("Sending address has balance: {0} and unconfirmed balance: {1}",
				        balance, unconfirmedBalance);
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	langUtil.getString("messaging.send.no.funds.confirmed.message", ownIdentity.getSendreceiveaddress()),
		        langUtil.getString("messaging.send.no.funds.confirmed"), JOptionPane.ERROR_MESSAGE);
		        
	            // Restore controls and move to the send cash tab etc.
		        this.sendButton.setEnabled(true);
				this.writeMessageTextArea.setEnabled(true);
				
				return;
		}
		
		
		String memoString = null;
		JsonObject jsonInnerMessage = null;
		
		if (sendAnonymously)
		{
			// Form an anonymous message
			jsonInnerMessage = new JsonObject();
			jsonInnerMessage.set("ver", 1d);
			jsonInnerMessage.set("message", textToSend);
			jsonInnerMessage.set("threadid", contactIdentity.getThreadID());
			if (sendReturnAddress)
			{
				jsonInnerMessage.set("returnaddress", ownIdentity.getSendreceiveaddress());
			}
			
			JsonObject jsonOuterMessage = new JsonObject();
			jsonOuterMessage.set("zelcashmsg", jsonInnerMessage);
			memoString = jsonOuterMessage.toString();			
		} else
		{
			// Sign a HEX encoded message ... to avoid possible UNICODE issues
			String signature = this.clientCaller.signMessage(
				ownIdentity.getSenderidaddress(), Util.encodeHexString(textToSend).toUpperCase());
			
			jsonInnerMessage = new JsonObject();
			jsonInnerMessage.set("ver", 1d);
			jsonInnerMessage.set("from", ownIdentity.getSenderidaddress());
			jsonInnerMessage.set("message", textToSend);
			jsonInnerMessage.set("sign", signature);
			JsonObject jsonOuterMessage = new JsonObject();
			jsonOuterMessage.set("zelcashmsg", jsonInnerMessage);
			memoString = jsonOuterMessage.toString();
		}
		
		final JsonObject jsonInnerMessageForFurtherUse = jsonInnerMessage;
		
		// Check the size of the message to be sent, error if it exceeds.
		final int maxSendingLength = 512;
		int overallSendingLength = memoString.getBytes("UTF-8").length; 
		if (overallSendingLength > maxSendingLength)
		{
			Log.warning("Text length of exceeding message: {0}", textToSend.length());
			int difference = Math.abs(maxSendingLength - overallSendingLength);
			// We give exact size and advice on reduction...
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		langUtil.getString("messaging.send.text.exceeding.message", 
        				overallSendingLength,
        				maxSendingLength,
        				difference),
	        	langUtil.getString("messaging.send.text.exceeding"), JOptionPane.ERROR_MESSAGE);
	        // Restore controls and exit
	        this.sendButton.setEnabled(true);
			this.writeMessageTextArea.setEnabled(true);
			return;
		}
			
		if (updateMessagingIdentityJustBeforeSend)
		{
			if (!contactIdentity.isGroup())
			{
			   	this.messagingStorage.updateContactIdentityForSenderIDAddress(
		            contactIdentity.getSenderidaddress(), contactIdentity);
			} else
			{
				this.messagingStorage.updateGroupContactIdentityForSendReceiveAddress(
					contactIdentity.getSendreceiveaddress(), contactIdentity);
			}
		}
		
		// Finally send the message		
		String tempOperationID = null;
		try
		{
			tempOperationID = this.clientCaller.sendMessage(
	    	    ownIdentity.getSendreceiveaddress(), contactIdentity.getSendreceiveaddress(), 
	    	    msgOptions.getAmountToSend(), msgOptions.getTransactionFee(), memoString);
		} catch (WalletCallException wce)
		{
			Log.error("Wallet call error in sending message: ", wce);
			
			sendResultLabel.setText(langUtil.getString("messaging.send.status.error"));
			JOptionPane.showMessageDialog(
				MessagingPanel.this.getRootPane().getParent(), 
				langUtil.getString("messaging.send.error.wce.message", contactIdentity,
						wce.getMessage()),
				langUtil.getString("messaging.send.error.in.sending"), JOptionPane.ERROR_MESSAGE);
			
			sendMessageProgressBar.setValue(0);						 
			sendButton.setEnabled(true);
			writeMessageTextArea.setEnabled(true);
			
			// Exit prematurely
			return;
		}
		
		final String operationStatusID = tempOperationID;
		
		// Start a data gathering thread specific to the operation being executed - this is done is a separate 
		// thread since the server responds more slowly during JoinSplits and this blocks he GUI somewhat.
		final DataGatheringThread<Boolean> opFollowingThread = new DataGatheringThread<Boolean>(
			new DataGatheringThread.DataGatherer<Boolean>() 
			{
				public Boolean gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					Boolean result = MessagingPanel.this.clientCaller.isSendingOperationComplete(operationStatusID);
					long end = System.currentTimeMillis();
					Log.info("Checking for messaging operation " + operationStatusID + " status done in " + (end - start) + "ms." );
					
					return result;
				}
			}, 
			this.errorReporter, 2000, true);

		// Start a timer to update the progress of the operation
		this.operationStatusTimer = new Timer(2000, new ActionListener() 
		{
			public int operationStatusCounter = 0;
			
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					Boolean opComplete = opFollowingThread.getLastData();
					
					if ((opComplete != null) && opComplete.booleanValue())
					{
						// End the special thread used to follow the operation
						opFollowingThread.setSuspended(true);
						
						boolean sendWasSuccessful = clientCaller.isCompletedOperationSuccessful(operationStatusID); 
						if (sendWasSuccessful)
						{
							sendResultLabel.setText(
								langUtil.getString("messaging.send.status.success"));
						} else
						{
							String errorMessage = clientCaller.getOperationFinalErrorMessage(operationStatusID); 
							sendResultLabel.setText(
								langUtil.getString("messaging.send.status.error"));
							JOptionPane.showMessageDialog(
								MessagingPanel.this.getRootPane().getParent(), 
								langUtil.getString("messaging.send.error.wce.message", 
										contactIdentity.getDiplayString(),
										errorMessage),
								langUtil.getString("messaging.send.error.in.sending"), JOptionPane.ERROR_MESSAGE);
						}
								
								
						// Restore controls etc. final actions - reenable
						sendMessageProgressBar.setValue(0);
						operationStatusTimer.stop();
						operationStatusTimer = null;
						sendButton.setEnabled(true);
						writeMessageTextArea.setEnabled(true);
						writeMessageTextArea.setText(""); // clear message from text area
					    
						if (sendWasSuccessful)
						{
						    // Save message as outgoing
							Message msg = new Message(jsonInnerMessageForFurtherUse);
							msg.setTime(new Date());
							msg.setDirection(DIRECTION_TYPE.SENT);
							// TODO: We can get the transaction ID for outgoing messages but is is probably unnecessary
							msg.setTransactionID(""); 
							messagingStorage.writeNewSentMessageForContact(contactIdentity, msg);
						}
					    
					    // Update conversation text pane
						displayMessagesForContact(contactIdentity);
								
					} else
					{
						// Update the progress
						sendResultLabel.setText(
							langUtil.getString("messaging.send.status.inprogress"));
						operationStatusCounter += 2;
						int progress = 0;
						if (operationStatusCounter <= 100)
						{
							progress = operationStatusCounter;
						} else
						{
							progress = 100 + (((operationStatusCounter - 100) * 6) / 10);
						}
						sendMessageProgressBar.setValue(progress);
					}
							
					MessagingPanel.this.repaint();
				} catch (Exception ex)
				{
					Log.error("Unexpected error in sending message: ", ex);
					MessagingPanel.this.errorReporter.reportError(ex);
				}
			}
		}); // End timer operation
		operationStatusTimer.setInitialDelay(0);
		operationStatusTimer.start();	    
	}
	
	
	private void collectAndStoreNewReceivedMessagesAndHandleErrors()
		throws Exception
	{
		try
		{
			synchronized (this.messageCollectionMutex)
			{
				// When a large number of messages has been accumulated, this operation partly
				// slows down blockchain synchronization. So messages are collected only when
				// sync is full.
				NetworkAndBlockchainInfo info = this.clientCaller.getNetworkAndBlockchainInfo();
				// If more than 60 minutes behind in the blockchain - skip collection
				if ((System.currentTimeMillis() - info.lastBlockDate.getTime()) > (60 * 60 * 1000))
				{
					Log.warning("Current blockchain synchronization date is {0}. Message collection skipped for now!",
				                new Date(info.lastBlockDate.getTime()));
					return;
				}
				
				// Call it for the own identity
				collectAndStoreNewReceivedMessages(null);
				
				// Call it for all existing groups
				for (MessagingIdentity id : this.messagingStorage.getContactIdentities(false))
				{
					if (id.isGroup())
					{
						collectAndStoreNewReceivedMessages(id);
					}
				}
			}
		} catch (Exception e)
		{
			if (Thread.currentThread() instanceof DataGatheringThread)
			{
				if (((DataGatheringThread)Thread.currentThread()).isSuspended())
				{
					// Just rethrow the exception
					throw e;
				}
			}
			
			Log.error("Unexpected error in gathering received messages (wrapper): ", e);
			this.errorReporter.reportError(e);
		}
	}
	
	
	private void collectAndStoreNewReceivedMessages(MessagingIdentity groupIdentity)
		throws IOException, WalletCallException, InterruptedException
	{
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		
		// Check to make sure the Z address of the messaging identity is valid
		if ((ownIdentity != null) && (!this.identityZAddressValidityChecked))
		{
			String ownZAddress = ownIdentity.getSendreceiveaddress();
			String[] walletZaddresses = this.clientCaller.getWalletZAddresses();

			boolean bFound = false;
			for (String address : walletZaddresses)
			{
				if (ownZAddress.equals(address))
				{
					bFound = true;
				}
			}
			
			if (!bFound)
			{
				JOptionPane.showMessageDialog(
					MessagingPanel.this.getRootPane().getParent(),
					langUtil.getString("messaging.collect.identity.not.found.message", ownZAddress,
							OSUtil.getSettingsDirectory() + File.separator + "messaging"),
					langUtil.getString("messaging.collect.identity.not.found"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			this.identityZAddressValidityChecked = true;
		}
		
		// Get the transaction IDs from all received transactions in the local storage
		// TODO: optimize/cache this
		Set<String> storedTransactionIDs = new HashSet<String>();
		for (MessagingIdentity identity : this.messagingStorage.getContactIdentities(true))
		{
			for (Message localMessage : this.messagingStorage.getAllMessagesForContact(identity))
			{
				if ((localMessage.getDirection() == DIRECTION_TYPE.RECEIVED) &&
					(!Util.stringIsEmpty(localMessage.getTransactionID())))
				{
					storedTransactionIDs.add(localMessage.getTransactionID());
				}
			}
		}
	 
		if (ownIdentity == null)
		{
			Log.warning("Own messaging identity does not exist yet. No received messages collected!");
			return;
		}

		String ZAddress = (groupIdentity != null) ? 
		    groupIdentity.getSendreceiveaddress() : ownIdentity.getSendreceiveaddress(); 
		// Get all known transactions received from the wallet
		// TODO: there seems to be no way to limit the number of transactions returned!
		JsonObject[] walletTransactions = this.clientCaller.getTransactionMessagingDataForZaddress(ZAddress);
		
		// Filter the transactions to obtain only those that have memos parsable as JSON
		// and being real messages. In addition only those remain that are not registered before
		List<Message> filteredMessages = new ArrayList<Message>();
		for (JsonObject trans : walletTransactions)
		{
			String memoHex = trans.getString("memo", "ERROR");
			String transactionID = trans.getString("txid",  "ERROR");
			if (!memoHex.equals("ERROR"))
			{
				String decodedMemo = Util.decodeHexMemo(memoHex);
				JsonObject jsonMessage = null;
				try
				{
					if (decodedMemo != null)
					{
						jsonMessage = Util.parseJsonObject(decodedMemo);
					}
				} catch (Exception ex)
				{
					Log.warningOneTime(
						"Decoded memo is not parsable: {0}, due to {1}: {2}", 
						decodedMemo, ex.getClass().getName(), ex.getMessage());
				}
				
				if ((jsonMessage != null) &&
				   ((jsonMessage.get("zelcashmsg") != null) &&
				   (!storedTransactionIDs.contains(transactionID))))
				{
					JsonObject innerZelCashmsg = jsonMessage.get("zelcashmsg").asObject();
					if (Message.isValidZelCashMessagingProtocolMessage(innerZelCashmsg))
					{
						// Finally test that the message has all attributes required
						Message message = new Message(innerZelCashmsg);
						// Set additional message attributes not available over the wire
						message.setDirection(DIRECTION_TYPE.RECEIVED);
						message.setTransactionID(transactionID);
						String UNIXDate = this.clientCaller.getWalletTransactionTime(transactionID);
						message.setTime(new Date(Long.valueOf(UNIXDate).longValue() * 1000L));						
						// TODO: additional sanity check that T/Z addresses are valid etc.
						filteredMessages.add(message);
					} else
					{
						// Warn of unexpected message content
						Log.warningOneTime(
							"Ignoring received message with invalid or incomplete content: {0}",
							jsonMessage.toString());
					}
				}	
			} // End if (!memoHex.equals("ERROR"))
		} // for (JsonObject trans : walletTransactions)
		
		MessagingOptions msgOptions = this.messagingStorage.getMessagingOptions();

		// Finally we have all messages that are new and unprocessed. For every message we find out
		// who the sender is, verify it and store it
		boolean bNewContactCreated = false;
		
		// Loop for processing standard (not anonymous messages)
		standard_message_loop:
		for (Message message : filteredMessages)
		{
			if (message.isAnonymous())
			{
				continue standard_message_loop;
			}
			
			MessagingIdentity contactID = 
				this.messagingStorage.getContactIdentityForSenderIDAddress(message.getFrom());
			
			// Check for ignored contact messages
			if ((groupIdentity == null) && (contactID == null))
			{
				MessagingIdentity ignoredContact = this.messagingStorage.getIgnoredContactForMessage(message);
				if (ignoredContact != null)
				{
					Log.warningOneTime("Message detected from an ignored contact. Message will be ignored. " +
				                       "Message: {0}, Ignored contact: {1}",
				                       message.toJSONObject(false).toString(), 
				                       ignoredContact.toJSONObject(false).toString());
					continue standard_message_loop;
				}
			}
			
			// Skip message if from an unknown user and options are not set
			if ((groupIdentity == null) && (contactID == null) && 
				(!msgOptions.isAutomaticallyAddUsersIfNotExplicitlyImported()))
			{
				Log.warningOneTime(
					"Message is from an unknown user, but options do not allow adding new users: {0}", 
					message.toJSONObject(false).toString());
				continue standard_message_loop;
			}
			
			if ((groupIdentity == null) && (contactID == null))
			{
				// Update list of contacts with an unknown remote user ... to be updated later
				Log.warning("Message is from unknown contact: {0} . " + 
						    "A new Unknown_xxx contact will be created!", 
						    message.toJSONObject(false).toString());
				contactID = this.messagingStorage.createAndStoreUnknownContactIdentity(message.getFrom());
			    bNewContactCreated = true;
			}
			
			// Verify the message signature
			if (this.clientCaller.verifyMessage(message.getFrom(), message.getSign(), 
				                                Util.encodeHexString(message.getMessage()).toUpperCase()))
			{
				// Handle the special case of a messaging identity sent as payload - update identity then
				if ((groupIdentity == null) && this.isZelCashIdentityMessage(message.getMessage()))
				{
					this.updateAndStoreExistingIdentityFromIDMessage(contactID, message.getMessage());
				}
				message.setVerification(VERIFICATION_TYPE.VERIFICATION_OK);
			} else
			{
				//Set verification status permanently - store even invalid messages
				Log.error("Message signature is invalid {0} . Message will be stored as invalid!", 
						   message.toJSONObject(false).toString());
				message.setVerification(VERIFICATION_TYPE.VERIFICATION_FAILED);
			}
			
		    this.messagingStorage.writeNewReceivedMessageForContact(
		    		(groupIdentity == null) ? contactID : groupIdentity, message);
		} // End for (Message message : filteredMessages)

		// Loop for processing anonymous messages
		anonymus_message_loop:
		for (Message message : filteredMessages)
		{
			if (!message.isAnonymous())
			{
				continue anonymus_message_loop;
			}
			
			// It is possible that it will find a normal identity to which we previously sent the first
			// anonymous message (send scenario) or maybe an anonymous identity created by incoming
			// message etc.
			MessagingIdentity anonContctID = this.messagingStorage.
				findAnonymousOrNormalContactIdentityByThreadID(message.getThreadID());
			
			// Check for ignored contact messages
			if ((groupIdentity == null) && (anonContctID == null))
			{
				MessagingIdentity ignoredContact = this.messagingStorage.getIgnoredContactForMessage(message);
				if (ignoredContact != null)
				{
					Log.warningOneTime("Message detected from an ignored contact. Message will be ignored. " +
				                       "Message: {0}, Ignored contact: {1}",
				                       message.toJSONObject(false).toString(), 
				                       ignoredContact.toJSONObject(false).toString());
					continue anonymus_message_loop;
				}
			}
			
			// Skip message if from an unknown user and options are not set
			if ((groupIdentity == null) && (anonContctID == null) && 
				(!msgOptions.isAutomaticallyAddUsersIfNotExplicitlyImported()))
			{
				Log.warningOneTime(
					"Anonymous message is from an unknown user, but options do not allow adding new users: {0}", 
					message.toJSONObject(false).toString());
				continue anonymus_message_loop;
			}
					
			if ((groupIdentity == null) && (anonContctID == null))
			{
				// Return address may be empty but we pass it
				anonContctID = this.messagingStorage.createAndStoreAnonymousContactIdentity(
					message.getThreadID(), message.getReturnAddress());
				Log.info("Created new anonymous contact identity: ", anonContctID.toJSONObject(false).toString());
				bNewContactCreated = true;
			} else if ((groupIdentity == null) && Util.stringIsEmpty(anonContctID.getSendreceiveaddress()))
			{
				if (!Util.stringIsEmpty(message.getReturnAddress()))
				{
					anonContctID.setSendreceiveaddress(message.getReturnAddress());
					this.messagingStorage.updateAnonymousContactIdentityForThreadID(
						anonContctID.getThreadID(), anonContctID);
					Log.info("Updated anonymous contact identity: ", anonContctID.toJSONObject(false).toString());
				}
			}

			this.messagingStorage.writeNewReceivedMessageForContact(
				(groupIdentity == null) ? anonContctID : groupIdentity, message);
		}
		
		if (bNewContactCreated)
		{
			SwingUtilities.invokeLater(new Runnable() 
			{	
				@Override
				public void run() 
				{
					try
					{
						MessagingPanel.this.contactList.reloadMessagingIdentities();
					} catch (Exception e)
					{
						Log.error("Unexpected error in reloading contacts after gathering messages: ", e);
						MessagingPanel.this.errorReporter.reportError(e);
					}
				}
			});
		}		
		
		// TODO: Call this only if anything was changed - e.g. new messages saved
		SwingUtilities.invokeLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				try
				{
					// Reload the messages for the currently selected user
					final MessagingIdentity selectedContact = MessagingPanel.this.contactList.getSelectedContact();
					if (selectedContact != null)
					{
						MessagingPanel.this.displayMessagesForContact(selectedContact);
					}
				} catch (Exception e)
				{
					Log.error("Unexpected error in updating message pane after gathering messages: ", e);
					MessagingPanel.this.errorReporter.reportError(e);
				}
			}
		});
	}
	
	
	/**
	 * Checks if a message contains a ZEL messaging identity in it.
	 * 
	 * @param message
	 * 
	 * @return true if a ZEL identity is inside
	 */
	public boolean isZelCashIdentityMessage(String message)
	{
		if (message == null)
		{
			return false;
		}
		
		if (!message.trim().startsWith("{"))
		{
			return false;
		}
		
		JsonObject jsonMessage = null;
		try
		{
			jsonMessage = Util.parseJsonObject(message);
		} catch (Exception ex)
		{
			return false;
		}
		
		if (jsonMessage.get("zelcashmessagingidentity") == null)
		{
			return false;
		}
		
		JsonObject innerMessage = jsonMessage.get("zelcashmessagingidentity").asObject();
		if ((innerMessage.get("nickname") == null)           ||
			(innerMessage.get("sendreceiveaddress") == null) ||
			(innerMessage.get("senderidaddress") == null))
		{
			return false;
		}
		
		// All conditions met - return true
		return true;
	}
	

	// Copies the fields sent over the wire - limited set - someday all fields will be sent
	// Sender ID address is assumed to be the same
	public void updateAndStoreExistingIdentityFromIDMessage(MessagingIdentity existingIdentity, String idMessage)
		throws IOException
	{
		MessagingIdentity newID = new MessagingIdentity(
			Util.parseJsonObject(idMessage).get("zelcashmessagingidentity").asObject());

		if (!Util.stringIsEmpty(newID.getSenderidaddress()))
		{
			existingIdentity.setSenderidaddress(newID.getSenderidaddress());
		}
		
		if (!Util.stringIsEmpty(newID.getSendreceiveaddress()))
		{
			existingIdentity.setSendreceiveaddress(newID.getSendreceiveaddress());
		}
		
		if (!Util.stringIsEmpty(newID.getNickname()))
		{
			existingIdentity.setNickname(newID.getNickname());
		}
		
		if (!Util.stringIsEmpty(newID.getFirstname()))
		{
			existingIdentity.setFirstname(newID.getFirstname());
		}
		
		if (!Util.stringIsEmpty(newID.getMiddlename()))
		{
			existingIdentity.setMiddlename(newID.getMiddlename());
		}
		
		if (!Util.stringIsEmpty(newID.getSurname()))
		{
			existingIdentity.setSurname(newID.getSurname());
		}
		
		this.messagingStorage.updateContactIdentityForSenderIDAddress(
			existingIdentity.getSenderidaddress(), existingIdentity);
	}
	
	
	public void addMessagingGroup()
	{
		try
		{
			CreateGroupDialog cgd = new CreateGroupDialog(
				this, this.parentFrame, this.messagingStorage, this.errorReporter, this.clientCaller, this.labelStorage);
			cgd.setVisible(true);
			
			if (!cgd.isOKPressed())
			{
				return;
			}
			
			// So a group is created - we need to ask the user if he wishes to send an identity message 
			MessagingIdentity createdGroup = cgd.getCreatedGroup();
			Object[] options = 
	        	{ 
	        		langUtil.getString("button.option.yes"),
	        		langUtil.getString("button.option.no")
	        	};
			int sendIDChoice = JOptionPane.showOptionDialog(
				this.parentFrame, 
				langUtil.getString("messaging.add.group.message", createdGroup.getDiplayString()),
				langUtil.getString("messaging.add.group.send.contact"), JOptionPane.YES_NO_OPTION,
		        JOptionPane.QUESTION_MESSAGE,
		        null,
		        options,
		        JOptionPane.NO_OPTION);
				
			// TODO: code duplication with import
			if (sendIDChoice == JOptionPane.YES_OPTION)
			{
				// Only a limited set of values is sent over the wire, due to the limit of 330
				// characters. // TODO: use protocol versions with larger messages
				MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
				JsonObject innerIDObject = new JsonObject();
				innerIDObject.set("nickname",           ownIdentity.getNickname());
				innerIDObject.set("firstname",          ownIdentity.getFirstname());
				innerIDObject.set("surname",            ownIdentity.getSurname());
				innerIDObject.set("senderidaddress",    ownIdentity.getSenderidaddress());
				innerIDObject.set("sendreceiveaddress", ownIdentity.getSendreceiveaddress());
				JsonObject outerObject = new JsonObject();
				outerObject.set("zelcashmessagingidentity", innerIDObject);
				String identityString = outerObject.toString();
				
				// Check and send the messaging identity as a message
				if (identityString.length() <= 330) // Protocol V1 restriction
				{
					this.sendMessage(identityString, createdGroup);
				} else
				{
					JOptionPane.showMessageDialog(
						this.parentFrame, 
						langUtil.getString("messaging.add.identity.large.message"), 
						langUtil.getString("messaging.add.identity.large"), JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		} catch (Exception ex)
		{
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	public JContactListPanel getContactList()
	{
		return this.contactList;
	}
	
	
	private Map<String, MessagingIdentity> getKnownSendersForGroup(MessagingIdentity group)
		throws IOException
	{
		List<Message> messages = this.messagingStorage.getAllMessagesForContact(group);
		
		// Analyze the received messages to extract from them messaging identities (if there are any)
		// TODO: This could be cached to optimize performance
		Map<String, MessagingIdentity> knownSenders = new HashMap<String, MessagingIdentity>();
		for (Message msg : messages)
		{
			if (isZelCashIdentityMessage(msg.getMessage()) && 
				((msg.getDirection() == DIRECTION_TYPE.SENT) || 
				 (msg.getVerification() == VERIFICATION_TYPE.VERIFICATION_OK)))
			{
				MessagingIdentity senderIdentity = new MessagingIdentity(
						Util.parseJsonObject(msg.getMessage()).get("zelcashmessagingidentity").asObject());
				knownSenders.put(senderIdentity.getSenderidaddress(), senderIdentity);
			}
		}
		
		return knownSenders;
	}
	
	
	public void sendIdentityMessageTo(MessagingIdentity contactIdentity)
		throws InterruptedException, IOException, WalletCallException
	{
		// Only a limited set of values is sent over the wire, due to the limit of 330
		// characters. // TODO: use protocol versions with larger messages
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		JsonObject innerIDObject = new JsonObject();
		innerIDObject.set("nickname",           ownIdentity.getNickname());
		innerIDObject.set("firstname",          ownIdentity.getFirstname());
		innerIDObject.set("surname",            ownIdentity.getSurname());
		innerIDObject.set("senderidaddress",    ownIdentity.getSenderidaddress());
		innerIDObject.set("sendreceiveaddress", ownIdentity.getSendreceiveaddress());
		JsonObject outerObject = new JsonObject();
		outerObject.set("zelcashmessagingidentity", innerIDObject);
		String identityString = outerObject.toString();
		
		// Check and send the messaging identity as a message
		if (identityString.length() <= 330) // Protocol V1 restriction
		{
			this.sendMessage(identityString, contactIdentity);
		} else
		{
			JOptionPane.showMessageDialog(
				this.parentFrame, 
				langUtil.getString("messaging.send.identity.large.message"), 
				langUtil.getString("messaging.send.identity.large"), JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
	
	
	public void shareFileViaIPFS()
	{
		try
		{
			String ipfsLink = this.ipfs.shareFileViaIPFS();
			Log.info("IPFS Link is: {0}", ipfsLink);
			
			if (ipfsLink != null)
			{
				String oldText = this.writeMessageTextArea.getText();
				oldText = oldText != null ? oldText : "";
				
				this.writeMessageTextArea.setText(oldText + "\n" + ipfsLink);
			}
		} catch (Exception ex)
		{
			Log.error("Unexpected error in sharing file via IPFS: ", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
}
