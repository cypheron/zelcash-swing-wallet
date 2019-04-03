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


import java.awt.Font;

import javax.swing.JOptionPane;

import com.cabecinha84.zelcashui.ZelCashJCheckBox;
import com.cabecinha84.zelcashui.ZelCashJFrame;
import com.cabecinha84.zelcashui.ZelCashJLabel;
import com.cabecinha84.zelcashui.ZelCashJPasswordField;


/**
 * Dialog to get the user password - to encrypt a wallet.
 */
public class PasswordEncryptionDialog
	extends PasswordDialog
{
	protected ZelCashJPasswordField passwordConfirmationField = null;

	private LanguageUtil langUtil;
	
	private ZelCashJCheckBox  acceptExperimentalFeatures = null;
	
	public PasswordEncryptionDialog(ZelCashJFrame parent)
	{
		super(parent);
		langUtil = LanguageUtil.instance();
		this.upperLabel.setText(langUtil.getString("dialog.password.encryption.upper.label.text"));
		
		ZelCashJLabel confLabel = new ZelCashJLabel(langUtil.getString("dialog.password.encryption.confirmation.label.text"));
		this.freeSlotPanel.add(confLabel);
		this.freeSlotPanel.add(passwordConfirmationField = new ZelCashJPasswordField(50));
		this.passwordLabel.setPreferredSize(confLabel.getPreferredSize());
		
		acceptExperimentalFeatures = new ZelCashJCheckBox(langUtil.getString("encryption.accept.experimental"));
		this.freeSlotPanel3.add(acceptExperimentalFeatures);
		
		this.setSize(850, 270);
		this.validate();
		this.repaint();
	}
	
	
	protected void processOK()
	{
		boolean acceptExperimental = this.acceptExperimentalFeatures.isSelected();
		if(!acceptExperimental) {
			JOptionPane.showMessageDialog(
				this.getParent(), 
				langUtil.getString("encryption.accept.experimental.message"),
				langUtil.getString("encryption.accept.experimental.title")	,
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		String password     = this.passwordField.getText();
		String confirmation = this.passwordConfirmationField.getText(); 
		
		if (password == null)
		{
			password = "";
		}
		
		if (confirmation == null)
		{
			confirmation = "";
		}

		if (!password.equals(confirmation))
		{
			JOptionPane.showMessageDialog(
				this.getParent(), 
				langUtil.getString("dialog.password.encryption.option.pane.mismatch.text"),
				langUtil.getString("dialog.password.encryption.option.pane.mismatch.title")	,
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		super.processOK();
	}
	
}
