package com.cloud.cloudactivity6;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TextFragment extends Fragment {

    private Button uploadTextBT, downloadTextBT;
    private TextView downloadTextTV;
    private EditText uploadTextET;
    private String key;
    private AmazonS3Client s3Client;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_text, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        uploadTextBT = view.findViewById(R.id.uploadBT);
        downloadTextBT = view.findViewById(R.id.downloadBT);
        uploadTextET = view.findViewById(R.id.uploadTextET);
        downloadTextTV = view.findViewById(R.id.downloadedTextTV);

        s3Client = new AmazonS3Client(Constants.creds);

        uploadTextBT.setOnClickListener(view1 -> {
            if (uploadTextET.getText().toString().trim().isEmpty()) {
                Toast.makeText(getActivity(), "Please enter some text to upload", Toast.LENGTH_SHORT).show();
            } else {
                if (uploadTextET.getText().toString().contains(" "))
                    key = "Text: " + uploadTextET.getText().toString()
                            .substring(0, uploadTextET.getText().toString().indexOf(' '));
                else key = "Text: " + uploadTextET.getText().toString().trim();
                uploadText();
            }
        });

        downloadTextBT.setOnClickListener(view12 -> {
            System.out.println("Downloading an object");
            S3Object fullObject = s3Client.getObject(new GetObjectRequest(Constants.BUCKET_NAME, key));
            System.out.println("Content-Type: " + fullObject.getObjectMetadata().getContentType());
            System.out.println("Content: ");

            BufferedReader reader = new BufferedReader(new InputStreamReader(fullObject.getObjectContent()));
            String line = null;
            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(line);
                downloadTextTV.setText(line.toString());
            }
        });
    }

    private void uploadText() {
        s3Client.setRegion(Region.getRegion(Regions.CA_CENTRAL_1));
        s3Client.putObject(Constants.BUCKET_NAME, key, uploadTextET.getText().toString().trim());
    }
}
