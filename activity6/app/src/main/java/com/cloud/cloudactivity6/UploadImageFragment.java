package com.cloud.cloudactivity6;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class UploadImageFragment extends Fragment {

    private ImageView uploadImageIV, downloadImageIV;
    private Uri filePath;
    private File file;
    private String key;
    private AmazonS3Client s3Client;

    ActivityResultLauncher<android.content.Intent> mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri uri = result.getData().getData();
                        filePath = result.getData().getData();
                        try {
                            file = FileUtil.from(getActivity(), filePath);
                            key = file.getName();
                            InputStream in;
                            in = getActivity().getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(in);
                            uploadImageIV.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), "An error occurred!", Toast.LENGTH_LONG).show();
                        }

                        uploadImage();
                    }
                }
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_upload_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button uploadImageBT = view.findViewById(R.id.uploadBT);
        Button downloadImageBT = view.findViewById(R.id.downloadBT);
        uploadImageIV = view.findViewById(R.id.uploadIV);
        downloadImageIV = view.findViewById(R.id.downloadIV);
        s3Client = new AmazonS3Client(Constants.creds);
        uploadImageBT.setOnClickListener(view1 -> {
            if (ContextCompat.checkSelfPermission(
                    getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                mStartForResult.launch(cameraIntent);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        downloadImageBT.setOnClickListener(view12 -> {
            downloadFile();
        });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getActivity().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void downloadFile() {
        if (filePath != null) {

            final String fileName = file.getName();
            try {
                final File localFile = File.createTempFile("images", CommonMethods.getFileExtension(filePath, getContext()));

                TransferUtility transferUtility =
                        TransferUtility.builder()
                                .context(getActivity())
                                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                                .s3Client(s3Client)
                                .defaultBucket(Constants.BUCKET_NAME)
                                .build();

                TransferObserver downloadObserver =
                        transferUtility.download(key, localFile);

                downloadObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (TransferState.COMPLETED == state) {
                            Toast.makeText(getActivity(), "Download Completed!", Toast.LENGTH_SHORT).show();

                            Log.e("TAG", "onStateChanged: " + fileName + "." + getFileExtension(filePath));
                            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                            downloadImageIV.setImageBitmap(bmp);
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                        int percentDone = (int) percentDonef;
                        Log.e("TAG", "onProgressChanged: ID:" + id + "|bytesCurrent: " + bytesCurrent + "|bytesTotal: " + bytesTotal + "|" + percentDone + "%");
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        ex.printStackTrace();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getActivity(), "Upload file before downloading", Toast.LENGTH_LONG).show();
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    mStartForResult.launch(cameraIntent);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                }
            });

    private void uploadImage() {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
        s3Client.setRegion(Region.getRegion(Regions.CA_CENTRAL_1));
        TransferUtility transferUtility = new TransferUtility(s3Client, getActivity());
        TransferObserver observer = transferUtility.upload(Constants.BUCKET_NAME, file.getName(), file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Toast.makeText(getActivity(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                } else if (state == TransferState.FAILED) {
                    Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                Toast.makeText(getActivity(), "Uploading in progress " + percentage + "%", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
