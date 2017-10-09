package edu.usfca.cs.dfs.client;

import com.google.protobuf.ByteString;
import edu.usfca.cs.dfs.controller.ControllerProtobuf;
import edu.usfca.cs.dfs.storage.StorageProtobuf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by npbandal on 10/1/17.
 */
public class ClientProtoBuf {

    public List<String> clientToController(int portnumber, String chunkname) {
        System.out.println(chunkname);
        List<String> hostnames = new ArrayList<>();
        try {
            Socket sockController = new Socket("localhost", portnumber);
            ControllerProtobuf.ClientTalk clientTalk
                    = ControllerProtobuf.ClientTalk.newBuilder()
                    .setChunkName(chunkname)
                    .build();
            ControllerProtobuf.ControllerMessagePB msgWrapper =
                    ControllerProtobuf.ControllerMessagePB.newBuilder()
                            .setClienttalk(clientTalk)
                            .build();
            msgWrapper.writeDelimitedTo(sockController.getOutputStream());


            //Hostnames back from controller
            ControllerProtobuf.ListOfHostnames listOfHostnames = ControllerProtobuf.ListOfHostnames
                    .parseDelimitedFrom(sockController.getInputStream());
            hostnames = listOfHostnames.getHostnamesList();


            sockController.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostnames;
    }


    public void protoBufToWriteintoStorageNode(String hostname, int portnumber, String filename, int chunkId, byte[] chunk) {
        Socket sockController = null;
        try {
            String s = new String(chunk);
            ByteString data = ByteString.copyFromUtf8(s);
            sockController = new Socket(hostname, portnumber);
            StorageProtobuf.StoreChunk storeChunkMsg
                    = StorageProtobuf.StoreChunk.newBuilder()
                    .setWritefilechunkName(filename)
                    .setChunkId(chunkId)
                    .setWritechunkdata(data)
                    .setReqtypewrite("write")
                    .build();
            StorageProtobuf.StorageMessagePB msgWrapper =
                    StorageProtobuf.StorageMessagePB.newBuilder()
                            .setStoreChunkMsg(storeChunkMsg)
                            .build();
            msgWrapper.writeDelimitedTo(sockController.getOutputStream());
            sockController.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendReadReqToStorageNode(String hostname, int portnumber, String filename, int chunkID) {
        try {

            Socket sockController = new Socket(hostname, portnumber);
            StorageProtobuf.RetrieveFile retrieveFile
                    = StorageProtobuf.RetrieveFile.newBuilder()
                    .setReadfileName(filename)
                    .setChunkId(chunkID)
                    .setReqtyperead("read")
                    .build();
            StorageProtobuf.StorageMessagePB msgWrapper =
                    StorageProtobuf.StorageMessagePB.newBuilder()
                            .setRetrieveChunkFileMsg(retrieveFile)
                            .build();
            msgWrapper.writeDelimitedTo(sockController.getOutputStream());

            //Receive chunks from storage nodes
            StorageProtobuf.StorageMessagePB recfilechunks = StorageProtobuf.StorageMessagePB
                    .parseDelimitedFrom(sockController.getInputStream());
            if (recfilechunks.hasRetrieveChunkFileMsg()) {
                StorageProtobuf.RetrieveFile retrivechunkfiledata = recfilechunks.getRetrieveChunkFileMsg();
                ByteString chunkdata = retrivechunkfiledata.getReadchunkdata();

                byte[] chunkbytes = chunkdata.toByteArray();
                System.out.println(new String(chunkbytes));

                StorageProtobuf.Profile.Builder profile = StorageProtobuf.Profile.newBuilder()
                        .setChunkdatat(chunkdata);

                String chunkname = filename + "ThanksGanesha" + chunkID;
                FileOutputStream output = new FileOutputStream(chunkname);
                profile.build().writeTo(output);

                sockController.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//
//    public void protoBufToSendHeartbeatFromStorageNodeToController(int portnumber, String msg) {
//        try {
//
////            ByteString data = ByteString.copyFrom(chunk);
//            Socket sockController = new Socket("localhost", portnumber);
//            StorageMessages.StoreChunk storeChunkMsg
//                    = StorageMessages.StoreChunk.newBuilder()
//                    .setFileName(msg)
//                    .build();
//            StorageMessages.StorageMessageWrapper msgWrapper =
//                    StorageMessages.StorageMessageWrapper.newBuilder()
//                            .setStoreChunkMsg(storeChunkMsg)
//                            .build();
//            msgWrapper.writeDelimitedTo(sockController.getOutputStream());
//            sockController.close();
//        } catch (IOException e) {
//
//        }

//    }

//    public void protoBufToReceiveHeartbeatFromStorageNodeAtController(int portnumber) throws IOException {
//        ServerSocket srvSocket = new ServerSocket(portnumber);
//        try {
//            Socket client = srvSocket.accept();
//            StorageMessages.StorageMessageWrapper msgWrapper
//                    = StorageMessages.StorageMessageWrapper.parseDelimitedFrom(
//                    client.getInputStream());
//            if (msgWrapper.hasStoreChunkMsg()) {
//                StorageMessages.StoreChunk storeChunkMsg
//                        = msgWrapper.getStoreChunkMsg();
//                System.out.println("Host Alive " + storeChunkMsg.getFileName());
//            }
//        } finally {
//            srvSocket.close();
//        }
//    }


}



