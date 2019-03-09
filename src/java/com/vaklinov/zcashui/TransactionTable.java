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
package com.vaklinov.zcashui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;

import com.cabecinha84.zelcashui.ZelCashJButton;
import com.cabecinha84.zelcashui.ZelCashJDialog;
import com.cabecinha84.zelcashui.ZelCashJFrame;
import com.cabecinha84.zelcashui.ZelCashJLabel;
import com.cabecinha84.zelcashui.ZelCashJMenuItem;
import com.cabecinha84.zelcashui.ZelCashJPanel;
import com.cabecinha84.zelcashui.ZelCashJScrollPane;

//TODO

/**
 * Table to be used for transactions - specifically.
 */
public class TransactionTable 
	extends DataTable 
{	
	public TransactionTable(final Object[][] rowData, final Object[] columnNames, 
			                final ZelCashJFrame parent, final ZCashClientCaller caller,
			                final ZCashInstallationObserver installationObserver)
	{
		super(rowData, columnNames);
		int accelaratorKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		final LanguageUtil langUtil = LanguageUtil.instance();
		ZelCashJMenuItem showDetails = new ZelCashJMenuItem("Show details...");
		showDetails.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, accelaratorKeyMask));
        popupMenu.add(showDetails);
        
        showDetails.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{

				try
				{
					int row = TransactionTable.this.convertRowIndexToModel(TransactionTable.this.getSelectedRow());
					
					String txID = TransactionTable.this.getModel().getValueAt(row, 6).toString();
					txID = txID.replaceAll("\"", ""); // In case it has quotes
					
					Log.info("Transaction ID for detail dialog is: " + txID);
					Map<String, String> details = caller.getRawTransactionDetails(txID);
					String rawTrans = caller.getRawTransaction(txID);
					
					DetailsDialog dd = new DetailsDialog(parent, details);
					dd.setVisible(true);
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					// TODO: report exception to user
				}

			}
		});
        
        
        ZelCashJMenuItem showInExplorer = new ZelCashJMenuItem(langUtil.getString("transactions.table.show.in.explorer"));
		showInExplorer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, accelaratorKeyMask));
        popupMenu.add(showInExplorer);
        
        showInExplorer.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{

				try
				{
					int row = TransactionTable.this.convertRowIndexToModel(TransactionTable.this.getSelectedRow());
					
					String txID = TransactionTable.this.getModel().getValueAt(row, 6).toString();
					txID = txID.replaceAll("\"", ""); // In case it has quotes
					
					Log.info("Transaction ID for block explorer is: " + txID);
					// https://explorer.zcha.in/transactions/<ID>
					String urlPrefix = "https://explorer.zel.cash/tx/";
					if (installationObserver.isOnTestNet())
					{
						urlPrefix = "https://testnet.zelcash.online/tx/";
					}
					
					Desktop.getDesktop().browse(new URL(urlPrefix + txID).toURI());
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					// TODO: report exception to user
				}

			}
		});
		
        ZelCashJMenuItem showMemoField = new ZelCashJMenuItem(langUtil.getString("transactions.table.memo.field"));
        showMemoField.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, accelaratorKeyMask));
	    popupMenu.add(showMemoField);
    
        showMemoField.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{

				Cursor oldCursor = parent.getCursor();
				try
				{
					int row = TransactionTable.this.convertRowIndexToModel(TransactionTable.this.getSelectedRow());
					
					String txID = TransactionTable.this.getModel().getValueAt(row, 6).toString();
					txID = txID.replaceAll("\"", ""); // In case it has quotes
					

					String acc = TransactionTable.this.getModel().getValueAt(row, 5).toString();
					// TODO: better way to remove a label if it preceeds
					if (acc.contains(" - "))
					{
						acc = acc.substring(acc.lastIndexOf(" - ") + 3);
					}
					
					acc = acc.replaceAll("\"", ""); // In case it has quotes
					
					boolean isZAddress = Util.isZAddress(acc);
					if (!isZAddress)
					{
				        JOptionPane.showMessageDialog(
					            parent,
					            langUtil.getString("transactions.table.memo.unavailable.text"),
					            langUtil.getString("transactions.table.memo.unavailable.title"),
					            JOptionPane.ERROR_MESSAGE);
					    return;
					}
					
					
					Log.info("Transaction ID for Memo field is: " + txID);
					Log.info("Account for Memo field is: " + acc);
					parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					// TODO: someday support outgoing Z transactions
					String MemoField = caller.getMemoField(acc, txID);
					parent.setCursor(oldCursor);
					Log.info("Memo field is: " + MemoField);
					
					if (MemoField != null)
					{
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(new StringSelection(MemoField), null);
						
						MemoField = Util.blockWrapString(MemoField, 80);
						JOptionPane.showMessageDialog(
							parent, 
							langUtil.getString("transactions.table.memo.clipboard.text", MemoField),
							langUtil.getString("transactions.table.memo.clipboard.title"),
							JOptionPane.PLAIN_MESSAGE);
					} else
					{
				        JOptionPane.showMessageDialog(
					            parent,
					            langUtil.getString("transactions.table.memo.field.missing.text"),
					            langUtil.getString("transactions.table.memo.field.missing.title"),
					            JOptionPane.ERROR_MESSAGE);
					}
				} catch (Exception ex)
				{
					parent.setCursor(oldCursor);
					Log.error("", ex);
					// TODO: report exception to user
				}
			}
        });
		
	} // End constructor


	
	
	private static class DetailsDialog
		extends ZelCashJDialog
	{
		public DetailsDialog(ZelCashJFrame parent, Map<String, String> details)
			throws UnsupportedEncodingException
		{
			LanguageUtil langUtil = LanguageUtil.instance();
			this.setTitle(langUtil.getString("transaction.table.details.dialog.title"));
			this.setSize(600,  310);
		    this.setLocation(100, 100);
			this.setLocationRelativeTo(parent);
			this.setModal(true);
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			
			this.getContentPane().setLayout(new BorderLayout(0, 0));
			
			ZelCashJPanel tempPanel = new ZelCashJPanel(new BorderLayout(0, 0));
			tempPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			ZelCashJLabel infoLabel = new ZelCashJLabel(
					langUtil.getString("transaction.table.details.dialog.info.label"));
			infoLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			tempPanel.add(infoLabel, BorderLayout.CENTER);
			this.getContentPane().add(tempPanel, BorderLayout.NORTH);
			
			String[] columns = langUtil.getString("transaction.table.details.dialog.column.names").split(":");
			String[][] data = new String[details.size()][2];
			int i = 0;
			int maxPreferredWidht = 400;
			for (Entry<String, String> ent : details.entrySet())
			{
				if (maxPreferredWidht < (ent.getValue().length() * 6))
				{
					maxPreferredWidht = ent.getValue().length() * 6;
				}
				
				data[i][0] = ent.getKey();
				data[i][1] = ent.getValue();
				i++;
			}
			
			Arrays.sort(data, new Comparator<String[]>() 
			{
			    public int compare(String[] o1, String[] o2)
			    {
			    	return o1[0].compareTo(o2[0]);
			    }

			    public boolean equals(Object obj)
			    {
			    	return false;
			    }
			});
			
			DataTable table = new DataTable(data, columns);
			table.getColumnModel().getColumn(0).setPreferredWidth(200);
			table.getColumnModel().getColumn(1).setPreferredWidth(maxPreferredWidht);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			ZelCashJScrollPane tablePane = new ZelCashJScrollPane(
				table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			
			this.getContentPane().add(tablePane, BorderLayout.CENTER);

			// Lower close button
			ZelCashJPanel closePanel = new ZelCashJPanel();
			closePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
			ZelCashJButton closeButon = new ZelCashJButton(langUtil.getString("transaction.table.details.dialog.button.close"));
			closePanel.add(closeButon);
			this.getContentPane().add(closePanel, BorderLayout.SOUTH);

			closeButon.addActionListener(new ActionListener()
			{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						DetailsDialog.this.setVisible(false);
						DetailsDialog.this.dispose();
					}
			});

		}
		
		
	}
}
