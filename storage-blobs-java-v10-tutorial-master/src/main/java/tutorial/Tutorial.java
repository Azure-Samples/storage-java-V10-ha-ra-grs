package tutorial;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlockBlobSimpleUploadOptions;
import com.azure.storage.blob.specialized.BlockBlobAsyncClient;
import com.azure.storage.common.StorageSharedKeyCredential;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Tutorial {

    static File createTempFile() throws IOException {

        // Here we are creating a temporary file to use for download and upload to Blob storage
        File sampleFile = null;
        sampleFile = File.createTempFile("HelloWorld", ".txt");
        System.out.println(">> Creating a sample file at: " + sampleFile.toString());
        Writer output = new BufferedWriter(new FileWriter(sampleFile));
        output.write("Hello Azure!");
        output.close();

        return sampleFile;
    }

    static void uploadFile(BlockBlobAsyncClient blob, File sourceFile) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8 * 1024 * 1024);
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(sourceFile.toPath());
        fileChannel.read(buffer, 0);
        blob.uploadWithResponse(new BlockBlobSimpleUploadOptions(
                Flux.just(buffer), buffer.array().length
        )).subscribe(response -> {
            System.out.println("Completed upload request.");
            System.out.println(response.getStatusCode());
        });
    }

    static void getBlob(BlockBlobAsyncClient blockBlobAsyncClient, File sourceFile) {
        blockBlobAsyncClient.downloadStreamWithResponse(
                new BlobRange(0, 4 * 1024 * 1024L),
                null, null, false)
                .doOnSubscribe(onSubscribe -> System.out.println("The blob was downloaded to " + sourceFile.getAbsolutePath()))
                .subscribe(transformer -> {
                    if (transformer.getRequest().getUrl().getHost().contains("-secondary")) {
                        System.out.println("Successfully used secondary pipeline.");
                    } else {
                        System.out.println("Successfully used primary pipeline.");
                    }
                    try {
                        AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(sourceFile.getPath()), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        transformer.getValue().subscribe(bufferValue -> {
                            channel.write(bufferValue, 0);
                            System.out.println("The blob was downloaded to " + sourceFile.getAbsolutePath());
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    transformer.getValue().subscribe();
                });
    }

    static void listBlobs(BlobContainerAsyncClient blobContainerAsyncClient) {
        // Each BlobContainerAsyncClient.listBlobs call return up to maxResultsPerPage (maxResults=10 passed into ListBlobOptions below).
        ListBlobsOptions options = new ListBlobsOptions();
        options.setMaxResultsPerPage(10);
        blobContainerAsyncClient.listBlobs(options).count().subscribe(count -> {
            if (count > 0) {
                blobContainerAsyncClient.listBlobs(options).subscribe(response -> {
                    String output = "Blob name: " + response.getName();
                    if (response.getSnapshot() != null) {
                        output += ", Snapshot: " + response.getSnapshot();
                    }
                    System.out.println(output);
                });
            } else {
                System.out.println("There are no more blobs to list off.");
            }
        });
    }

    static void deleteBlob(BlockBlobAsyncClient blockBlobAsyncClient) {
        // Delete the blob
        blockBlobAsyncClient.delete().doOnError(onError ->
                System.out.println(">> An error encountered during deleteBlob: " + onError.getMessage())
        ).subscribe(subscriber ->
                System.out.println(">> Blob deleted: " + blockBlobAsyncClient.getBlobUrl())
        );
    }


    public static void main(String[] args) {
        // Creating a sample file to use in the sample
        File sampleFile = null;

        try {
            sampleFile = createTempFile();

            File downloadedFile = File.createTempFile("downloadedFile", ".txt");

            // Retrieve the credentials and initialize SharedKeyCredentials
            String accountName = System.getenv("AZURE_STORAGE_ACCOUNT");
            String accountKey = System.getenv("AZURE_STORAGE_ACCESS_KEY");
            StorageSharedKeyCredential creds = new StorageSharedKeyCredential(accountName, accountKey);

            // Create a BlobServiceAsyncClient to call the Blob service. We will also use this to construct the BlobContainerAsyncClient
            BlobServiceAsyncClient blobServiceAsyncClient = new BlobServiceClientBuilder()
                    .endpoint(String.format("https://%s.blob.core.windows.net", accountName))
                    .credential(creds)
                    .buildAsyncClient();

            // Let's create a container using a blocking call to Azure Storage
            // If container exists, we'll catch and continue
            Long count = blobServiceAsyncClient.listBlobContainers()
                    .takeWhile(blobContainerItem -> "tutorial".equals(blobContainerItem.getName()))
                    .count().block();
            BlobContainerAsyncClient blobContainerAsyncClient =
                    count > 0 ? blobServiceAsyncClient.getBlobContainerAsyncClient("tutorial")
                            : blobServiceAsyncClient.createBlobContainer("tutorial").block();

            // Create a BlockBlobAsyncClient to run operations on Blobs
            BlockBlobAsyncClient blockBlobAsyncClient = blobContainerAsyncClient
                    .getBlobAsyncClient("HelloWorld.txt").getBlockBlobAsyncClient();

            // Listening for commands from the console
            System.out.println("Enter a command");
            System.out.println("(P)utBlob | (L)istBlobs | (G)etBlob | (D)eleteBlobs | (E)xitSample");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {

                System.out.println("# Enter a command : ");
                String input = reader.readLine();

                switch (input) {
                    case "P":
                        System.out.println("Uploading the sample file into the container: " + blobContainerAsyncClient.getBlobContainerUrl());
                        uploadFile(blockBlobAsyncClient, sampleFile);
                        break;
                    case "L":
                        System.out.println("Listing blobs in the container: " + blobContainerAsyncClient.getBlobContainerUrl());
                        listBlobs(blobContainerAsyncClient);
                        break;
                    case "G":
                        System.out.println("Get the blob: " + blockBlobAsyncClient.getBlobUrl());
                        getBlob(blockBlobAsyncClient, downloadedFile);
                        break;
                    case "D":
                        System.out.println("Delete the blob: " + blockBlobAsyncClient.getBlobUrl());
                        deleteBlob(blockBlobAsyncClient);
                        System.out.println();
                        break;
                    case "E":
                        System.out.println("Cleaning up the sample and exiting!");
                        blobContainerAsyncClient.delete().block();
                        downloadedFile.delete();
                        System.exit(0);
                        break;
                    default:
                        break;
                }
            }
        } catch (MalformedURLException e) {
            System.out.println("Invalid URI provided");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}