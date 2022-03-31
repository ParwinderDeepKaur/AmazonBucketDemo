package com.cloud.cloudactivity6;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.IOException;

public class TextFileFragment extends Fragment {

    private TextView uploadFileTV, downloadFileTV;
    private final BasicAWSCredentials creds = new BasicAWSCredentials(Constants.AWS_ACCESS_KEY, Constants.AWS_SECRET_KEY);
    private File file, downloadedFile, readFile;
    private String key;
    private AmazonS3Client s3Client;

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        file = FileUtil.from(getActivity(), result.getData().getData());
                        uploadFileTV.setText(file.getName());
                        key = file.getName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    uploadFile();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_upload_text_file, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button uploadFileBT = view.findViewById(R.id.uploadBT);
        Button downloadFileBT = view.findViewById(R.id.downloadBT);
        uploadFileTV = view.findViewById(R.id.uploadFileTV);
        downloadFileTV = view.findViewById(R.id.downloadFileTV);

        s3Client = new AmazonS3Client(creds);

        uploadFileBT.setOnClickListener(view1 -> {
            if (checkPermission()) {
                openFileChooserIntent();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        downloadFileBT.setOnClickListener(view13 -> {
            downloadFile();
        });

        downloadFileTV.setOnClickListener(view12 -> {
            readFile();
        });
    }

    private void downloadFile() {
        TransferUtility utility =
                TransferUtility.builder()
                        .context(getActivity())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .defaultBucket(Constants.BUCKET_NAME)
                        .build();

        downloadedFile = new File(getActivity().getExternalFilesDir(null).toString() + "/" + key);

        // Initiate the download
        TransferObserver observer = utility.download(key, file);
        observer.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Toast.makeText(getActivity(), "Download Completed!", Toast.LENGTH_SHORT).show();

                    //Log.e("TAG", "onStateChanged: " + downloadedFile.getName());
                    downloadFileTV.setText(downloadedFile.getName());

                    File dir = new File(getActivity().getCacheDir().getAbsolutePath());
                    if (dir.exists()) {
                        for (File f : dir.listFiles()) {
                            f.getName();
                            if (f.getName().equals(file.getName())) {
                                readFile = f;
                            }
                        }
                    }
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

    }

    private void readFile() {
        Intent intent = new Intent();
        Uri uri = Uri.fromFile(readFile);
        intent.setData(uri);
        startActivity(intent);
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void openFileChooserIntent() {
        String[] mimeTypes =
                {"application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .doc & .docx
                        "application/vnd.ms-powerpoint","application/vnd.openxmlformats-officedocument.presentationml.presentation", // .ppt & .pptx
                        "application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xls & .xlsx
                        "text/plain",
                        "application/pdf",
                        "application/zip"};

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 0) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            String mimeTypesStr = "";
            for (String mimeType : mimeTypes) {
                mimeTypesStr += mimeType + "|";
            }
            intent.setType(mimeTypesStr.substring(0,mimeTypesStr.length() - 1));
        }
        mStartForResult.launch(intent);
    }


    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/plain");
                    mStartForResult.launch(intent);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                }
            });

    private void uploadFile() {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
        s3Client.setRegion(Region.getRegion(Regions.CA_CENTRAL_1));
        TransferUtility transferUtility = new TransferUtility(s3Client, getActivity());
        TransferObserver observer = transferUtility.upload(Constants.BUCKET_NAME, file.getName(), file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Toast.makeText(getActivity(), "File uploaded successfully", Toast.LENGTH_SHORT).show();
                } else if (state == TransferState.FAILED) {
                    Toast.makeText(getActivity(), "Failed to upload file", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }

        });
    }
}
