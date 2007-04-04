// $Id: AntiTheftGui.java,v 1.2 2007-04-04 22:06:22 idgay Exp $

/*									tab:4
 * "Copyright (c) 2000-2003 The Regents of the University  of California.  
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Copyright (c) 2002-2007 Intel Corporation
 * All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE     
 * file. If you do not find these files, copies can be found by writing to
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300, Berkeley, CA, 
 * 94704.  Attention:  Intel License Inquiry.
 */


/**
 * Description:
 * The GUI for the AntiTheft application.
 *
 * @author Bret Hull
 * @author David Gay
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import net.tinyos.message.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;

public class AntiTheftGui implements MessageListener, Messenger {
    MoteIF mote;
    JFrame frame;
    JTextArea mssgArea;
    JTextField fieldInterval;
    JCheckBox detDarkCb, detAccelCb, repLedCb, repSirenCb, repServerCb,
	repNeighboursCb;

    public AntiTheftGui() {
	try {
	    guiInit();
	    mote = new MoteIF(this);
	    mote.registerListener(new AlertMsg(), this);
	}
	catch(Exception e) {
	    e.printStackTrace();
	    System.exit(2);
	}
    }

    private void guiInit() throws Exception {
	JPanel mainPanel = new JPanel(new BorderLayout());
	mainPanel.setMinimumSize(new Dimension(500, 250));
	mainPanel.setPreferredSize(new Dimension(500, 300));

	JScrollPane mssgPanel = new JScrollPane();
	mssgPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	mssgPanel.setAutoscrolls(true);
	mssgArea = new JTextArea();
	mssgArea.setFont(new java.awt.Font("Monospaced", Font.PLAIN, 20));
	mainPanel.add(mssgPanel, BorderLayout.CENTER);
	mssgPanel.getViewport().add(mssgArea, null);
	
	BagPanel buttonPanel = new BagPanel();
	GridBagConstraints c = buttonPanel.c;

	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridwidth = GridBagConstraints.REMAINDER;

	buttonPanel.makeLabel("Detection", JLabel.CENTER);
	c.gridwidth = GridBagConstraints.RELATIVE;
	detDarkCb = buttonPanel.makeCheckBox("Dark", true);
	c.gridwidth = GridBagConstraints.REMAINDER;
	detAccelCb = buttonPanel.makeCheckBox("Movement", false);
	buttonPanel.addSeparator(SwingConstants.HORIZONTAL);


	buttonPanel.makeLabel("Theft Reports", JLabel.CENTER);
	c.gridwidth = GridBagConstraints.RELATIVE;
	repLedCb = buttonPanel.makeCheckBox("LED", true);
	c.gridwidth = GridBagConstraints.REMAINDER;
	repSirenCb = buttonPanel.makeCheckBox("Siren", false);
	c.gridwidth = GridBagConstraints.RELATIVE;
	repServerCb = buttonPanel.makeCheckBox("Server", false);
	c.gridwidth = GridBagConstraints.REMAINDER;
	repNeighboursCb = buttonPanel.makeCheckBox("Neighbours", false);
	buttonPanel.addSeparator(SwingConstants.HORIZONTAL);

	buttonPanel.makeLabel("Interval", JLabel.CENTER);
	fieldInterval = buttonPanel.makeTextField(10, null);
	fieldInterval.setText(Integer.toString(Constants.DEFAULT_CHECK_INTERVAL));

	// Send settings button
	ActionListener settingsAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    updateSettings();
		}
	    };
	buttonPanel.makeButton("Update", settingsAction);

	mainPanel.add(buttonPanel, BorderLayout.EAST);

	// The frame part
	frame = new JFrame("AntiTheft");
	frame.setSize(mainPanel.getPreferredSize());
	frame.getContentPane().add(mainPanel);
	frame.setVisible(true);
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) { System.exit(0); }
	    });
    }

    void error(String msg) {
	JOptionPane.showMessageDialog(frame, msg, "Error",
				      JOptionPane.ERROR_MESSAGE);
    }

    public void updateSettings() { 
	SettingsMsg smsg = new SettingsMsg();
	short alert = 0;
	short detect = 0;
	int checkInterval = Constants.DEFAULT_CHECK_INTERVAL;

	/* Extract current interval value, ignoring bad values */
	String intervalS = fieldInterval.getText().trim();
	try {
	    int newInterval = Integer.parseInt(intervalS);
	    if (newInterval < 10) throw new NumberFormatException();
	    checkInterval = newInterval;
	}
	catch (NumberFormatException e) { 
	    /* Reset field when value is bad */
	    fieldInterval.setText("" + checkInterval);
	}

	if (repLedCb.isSelected())
	    alert |= Constants.ALERT_LEDS;
	if (repSirenCb.isSelected())
	    alert |= Constants.ALERT_SOUND;
	if (repNeighboursCb.isSelected())
	    alert |= Constants.ALERT_RADIO;
	if (repServerCb.isSelected())
	    alert |= Constants.ALERT_ROOT;
	if (alert == 0) {
	    /* If nothing select, force-select LEDs */
	    alert = Constants.ALERT_LEDS;
	    repLedCb.setSelected(true);
	}

	if (detDarkCb.isSelected())
	    detect |= Constants.DETECT_DARK;
	if (detAccelCb.isSelected())
	    detect |= Constants.DETECT_ACCEL;
	if (detect == 0) {
	    /* If no detection selected, force-select dark */
	    detect = Constants.DETECT_DARK;
	    detDarkCb.setSelected(true);
	}

	smsg.set_alert(alert);
	smsg.set_detect(detect);
	smsg.set_checkInterval(checkInterval);

	try {
	    mote.send(MoteIF.TOS_BCAST_ADDR, smsg);
	}
	catch (IOException e) {
	    error("Cannot send message to mote");
	}
    }

    public static void main(String[] args) {
	AntiTheftGui me = new AntiTheftGui();
    }

    synchronized public void messageReceived(int dest_addr, Message msg) {
	if (msg instanceof AlertMsg) {
	    AlertMsg alertMsg = (AlertMsg)msg;

	    String warning = "Theft of " + alertMsg.get_stolenId() + "\n";
	    mssgArea.append(warning);
	    mssgArea.setCaretPosition(mssgArea.getDocument().getLength());
	}
    }

    public void message(String s) {
	error(s);
    }
}
