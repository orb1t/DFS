package edu.usfca.cs.dfs.client;

import com.google.protobuf.ByteString;
import edu.usfca.cs.dfs.controller.ControllerProtobuf;
import edu.usfca.cs.dfs.storage.StorageProtobuf;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by npbandal on 10/1/17.
 */
public class ClientProtoBuf {

    public Map<String, Long> clientToControllerwrite(String controllerHost, int portnumber, String chunkname, int chunknum, int chunkId, String reqType) {
        System.out.println(chunkname);
        Map<String, Long> hostnames = new LinkedHashMap<>();
        try {
            Socket sockController = sendReqToController(controllerHost, portnumber, chunkname, chunknum, chunkId, reqType);

//            Hostnames back from controller
            ControllerProtobuf.ListOfHostnames listOfHostnames = ControllerProtobuf.ListOfHostnames
                    .parseDelimitedFrom(sockController.getInputStream());
            hostnames = listOfHostnames.getHostnamesMap();

            sockController.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostnames;
    }

    public Map<String, String> clientToControllerread(String controllerHost, int portnumber, String chunkname, int chunknum, int chunkId, String reqType) {
        System.out.println(chunkname);
        Map<String, String> filesHostnames = new TreeMap<>();
        try {

            Socket sockController = sendReqToController(controllerHost, portnumber, chunkname, chunknum, chunkId, reqType);

            //            Hostnames back from controller with file name
            ControllerProtobuf.HostNamesFiles fileshosts = ControllerProtobuf.HostNamesFiles
                    .parseDelimitedFrom(sockController.getInputStream());
            filesHostnames = fileshosts.getHostNameFileMap();

            sockController.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return filesHostnames;
    }

    public String clientToControllerreadfilecurrupt(String controllerHost, int portnumber, String chunkname, String corruptHostname) {
        String hostnameFileCorrupt = null;
        try {

            Socket sockController = new Socket(controllerHost, portnumber);
            ControllerProtobuf.ClientTalk clientTalk
                    = ControllerProtobuf.ClientTalk.newBuilder()
                    .setChunkName(chunkname)
                    .setHostName(corruptHostname)
                    .build();
            ControllerProtobuf.ControllerMessagePB msgWrapper =
                    ControllerProtobuf.ControllerMessagePB.newBuilder()
                            .setClienttalk(clientTalk)
                            .build();
            msgWrapper.writeDelimitedTo(sockController.getOutputStream());


            //            Hostnames back from controller with file name
            ControllerProtobuf.ClientTalk reshostname = ControllerProtobuf.ClientTalk
                    .parseDelimitedFrom(sockController.getInputStream());
            hostnameFileCorrupt = reshostname.getHostName();

            sockController.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostnameFileCorrupt;
    }

    private Socket sendReqToController(String controllerHost, int portnumber, String chunkname, int chunknum, int chunkId, String reqType) throws IOException {
        Socket sockController = new Socket(controllerHost, portnumber);
        ControllerProtobuf.ClientTalk clientTalk
                = ControllerProtobuf.ClientTalk.newBuilder()
                .setChunkName(chunkname)
                .setNumChunks(chunknum)
                .setChunkId(chunkId)
                .setReqtype(reqType)
                .build();
        ControllerProtobuf.ControllerMessagePB msgWrapper =
                ControllerProtobuf.ControllerMessagePB.newBuilder()
                        .setClienttalk(clientTalk)
                        .build();
        msgWrapper.writeDelimitedTo(sockController.getOutputStream());
        return sockController;
    }

    public void protoBufToWriteintoStorageNode(String hostname, int portnumber, String filename, int chunkId, byte[] chunk, List<String> hostReplica) {
//    public void protoBufToWriteintoStorageNode(String hostname, int portnumber, String filename, int chunkId, byte[] chunk) {
        try {
            String s = new String(chunk);
            ByteString data = ByteString.copyFromUtf8(s);

            Socket sockController = new Socket(hostname, portnumber);

            StorageProtobuf.StoreChunk storeChunkMsg
                    = StorageProtobuf.StoreChunk.newBuilder()
                    .setWritefilechunkName(filename)
                    .setChunkId(chunkId)
                    .setReqTypeWrite("write")
                    .setWritechunkdata(data)
                    .addAllHostReplica(hostReplica)
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

    public byte[] sendReadReqToStorageNode(String hostname, int portnumber, String chunkname) throws IOException {
        Socket sockController = null;
        String checksumstorage = null;
        String checksumclient = null;
        byte[] chunkbytes = null;
        sockController = new Socket(hostname, portnumber);
        StorageProtobuf.RetrieveFile retrieveFile
                = StorageProtobuf.RetrieveFile.newBuilder()
                .setReadfileName(chunkname)
                .setReqTypeRead("read")
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
            chunkbytes = chunkdata.toByteArray();
            checksumstorage = retrivechunkfiledata.getChecksum();
            System.out.println("checksum storage: " + checksumstorage);

            checksumclient = DigestUtils.md5Hex(chunkbytes);
            System.out.println("checksum client: " + checksumclient);

        }
        if (checksumstorage != null) {
            if (checksumstorage.equals(checksumclient)) {
                FileOutputStream fop = new FileOutputStream("client_checksum_" + chunkname);
                fop.write(checksumstorage.getBytes());
                fop.flush();
                fop.close();
                return chunkbytes;
            } else {
                return null;
            }
        }
        return chunkbytes;
    }
}



