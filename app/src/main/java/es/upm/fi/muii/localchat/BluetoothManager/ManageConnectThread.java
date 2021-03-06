/**
 * Localchat
 *
 * @author Ignacio Molina Cuquerella
 * @author Claudiu Barzu
 */

package es.upm.fi.muii.localchat.BluetoothManager;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import es.upm.fi.muii.localchat.DeviceListActivity;
import es.upm.fi.muii.localchat.chat.Conversation;
import es.upm.fi.muii.localchat.profile.Profile;
import es.upm.fi.muii.localchat.utils.AudioRecorder;

/**
 * Created by claudiu on 15/12/2015.
 */

public class ManageConnectThread extends Thread {

    private BluetoothSocket socket;
    private Handler mHandler;

    public ManageConnectThread(BluetoothSocket aSocket,Handler aHandler ) {
        this.socket = aSocket;
        mHandler = aHandler;
    }


    public void run () {

        InputStream io = null;

        try {

            io = socket.getInputStream();

            byte [] longitudBytes = new byte[4];
            io.read(longitudBytes);
            int longitud = new BigInteger(longitudBytes).intValue();
            byte [] mensaje = new byte[longitud];
            int read = 0;
            while (read < longitud) {

                read += io.read(mensaje, read, longitud-read);
            }

            NetworkMessage readMessage= (NetworkMessage) NetworkMessage.deserialize(mensaje);

            readMessage.setWriter(socket.getRemoteDevice().getAddress());

            if (readMessage.messageType() != 3) {

                if (readMessage.messageType() == 2) { //is an audio chat

                    Map<String, byte[]> audio = (Map<String, byte[]>) readMessage.getMessage();
                    String filename = AudioRecorder.writeAudioToFile(audio.get(audio.keySet().iterator().next()));

                    audio = new HashMap<>(1);
                    audio.put(filename, null);
                    readMessage.setMessage(audio);
                }

                // Send the name of the connected device back to the UI Activity
                Message msg = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putSerializable("mensaje_recibido", readMessage);
                bundle.putString("user", socket.getRemoteDevice().getAddress());
                bundle.putString("chatroom", readMessage.getTarget());
                Log.d("MessageTarget", readMessage.getTarget());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                Log.d("ManageConnectThread", readMessage.toString());
                socket.close();

            } else if (readMessage.messageType() == 3) { // Profile type

                Conversation conv = DeviceListActivity.conversations.get(socket.getRemoteDevice().getAddress());

                if (conv != null) {

                    Profile profile = (Profile) readMessage.getMessage();
                    conv.setProfile(profile);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}