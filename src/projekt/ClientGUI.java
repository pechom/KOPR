package projekt;

import debug.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

public class ClientGUI extends javax.swing.JFrame {

    private int threadCount;
    private String umiestnenie;
    private File file;
    private int fileLength;
    private ProgressSwingWorker worker;

    public ClientGUI() { //dam tu listenery
        initComponents();
        cancelButton.addActionListener((ActionEvent e) -> {
            runButton.setEnabled(false);
            pauseButton.setEnabled(false);
            cancelButton.setEnabled(false);
            cancel();//ukoncit a poslat serveru "cancel" aby vymazal subor
            runButton.setEnabled(true);
        });

        pauseButton.addActionListener((ActionEvent e) -> {
            runButton.setEnabled(false);
            pauseButton.setEnabled(false);
            cancelButton.setEnabled(false);
            if (!worker.isDone()) {
                worker.cancel(true); 
            }
            runButton.setEnabled(true); //uz je obsluzene zrusenie, mozno zase pokracovat
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pauseButton = new javax.swing.JButton();
        runButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        umiestnenieTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        vlaknaSpinner = new javax.swing.JSpinner();
        uploadProgressBar = new javax.swing.JProgressBar();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        pauseButton.setText("Pause");
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        runButton.setText("Run");

        cancelButton.setText("Cancel");

        jLabel1.setText("umiestnenie súboru:");

        umiestnenieTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umiestnenieTextFieldActionPerformed(evt);
            }
        });

        jLabel3.setText("počet vláken:");

        jLabel4.setText("upload súboru:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(38, 38, 38)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(uploadProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                            .addComponent(vlaknaSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(umiestnenieTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(175, 175, 175)
                        .addComponent(pauseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(umiestnenieTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(53, 53, 53)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(vlaknaSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(47, 47, 47)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(uploadProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pauseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(runButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 40, 40))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pauseButtonActionPerformed

    private void umiestnenieTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_umiestnenieTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_umiestnenieTextFieldActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ClientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ClientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ClientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ClientGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        ClientGUI gui = new ClientGUI();
        java.awt.EventQueue.invokeLater(() -> {
            gui.setVisible(true);
            try {
                gui.runUpload();
            } catch (InterruptedException ex) {
                Logger.getLogger(ClientGUI.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("posielanie bolo prerušené");
            }
            gui.forcedExit(); 
        });
    }

    public void forcedExit() { // to iste ako cancel
        System.out.println("gui 207");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cancelButton.isEnabled() && !runButton.isEnabled()) {
                    System.out.println("gui 213");
                    cancel();
                }
                setVisible(false);
                dispose();
            }
        });

    }

    private void runUpload() throws InterruptedException {
        runButton.addActionListener((ActionEvent e) -> {
            try {
                vlaknaSpinner.commitEdit();
                runButton.setEnabled(false);
                pauseButton.setEnabled(true);
                cancelButton.setEnabled(true);
            } catch (ParseException ex) {
                Logger.getLogger(ClientGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            threadCount = (Integer) vlaknaSpinner.getValue(); //pocet vlaken
            umiestnenie = umiestnenieTextField.getText();
            file = new File(umiestnenie);
            if (file.isFile() && (!file.isDirectory())) {
                fileLength = (int) file.length();
                uploadProgressBar.setMinimum(0);
                uploadProgressBar.setMaximum(fileLength);
                worker = new ProgressSwingWorker(uploadProgressBar, file, threadCount);
                worker.execute();
                System.out.println("gui 242");
            }
        });
    }

    private void cancel() { //poslem spravu vymaz a skoncim ako pri preruseni
        System.out.println("gui 248");
        worker.setFullCancel(true);  //workera upozornim nech posle spravu serveru
        if (!worker.isDone()) {
            worker.cancel(true);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JButton pauseButton;
    private javax.swing.JButton runButton;
    private javax.swing.JTextField umiestnenieTextField;
    private javax.swing.JProgressBar uploadProgressBar;
    private javax.swing.JSpinner vlaknaSpinner;
    // End of variables declaration//GEN-END:variables

}
