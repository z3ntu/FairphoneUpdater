/*
 * Copyright (C) 2013 Fairphone Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fairphone.updater.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

import com.fairphone.updater.R;

public class RSAUtils
{

    private static final String TAG = RSAUtils.class.getSimpleName();

    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

// --Commented out by Inspection START (06/02/2015 12:27):
//    public static PublicKey readPublicKeyFormCertificate(Context context, int certificateResourceId) throws IOException, CertificateException
//    {
//        InputStream in = context.getResources().openRawResource(certificateResourceId);
//        byte[] buff = new byte[Utils.BUFFER_SIZE_4_KBYTES];
//        int bytesRead;
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        while ((bytesRead = in.read(buff)) != -1)
//        {
//            out.write(buff, 0, bytesRead);
//        }
//
//        byte[] publicKeyBytes = out.toByteArray();
//
//        CertificateFactory cf = CertificateFactory.getInstance("X509");
//        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(publicKeyBytes));
//
//        PublicKey pubKey = cert.getPublicKey();
//        return pubKey;
//    }
// --Commented out by Inspection STOP (06/02/2015 12:27)

    private static PublicKey readPublicKeyFromPemFormat(Context context) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {

        InputStream in = context.getResources().openRawResource(R.raw.public_key);
        BufferedReader pemReader = new BufferedReader(new InputStreamReader(in));

        StringBuilder content = new StringBuilder();
        String line;
        while ((line = pemReader.readLine()) != null)
        {
            if (line.contains("-----BEGIN PUBLIC KEY-----"))
            {
                while ((line = pemReader.readLine()) != null)
                {
                    if (line.contains("-----END PUBLIC KEY"))
                    {
                        break;
                    }
                    content.append(line.trim());
                }
                break;
            }
        }
        if (line == null)
        {
            throw new IOException("PUBLIC KEY" + " not found");
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decode(content.toString(), Base64.DEFAULT)));
    }

    private static byte[] readSignature(String input) throws IOException
    {
        File sigFile = new File(input);
        int fileSize = (int) sigFile.length();
        FileInputStream signStream = new FileInputStream(sigFile);
        byte[] signBytes = new byte[fileSize];
        int readBytes = signStream.read(signBytes);
        signStream.close();
        if(readBytes != fileSize){
            throw new IOException("Read failure");
        }
        return signBytes;
    }

    private static boolean verifySignature(String input, byte[] sign, PublicKey pubKey) throws Exception
    {
        Signature sg = Signature.getInstance(RSAUtils.SIGNATURE_ALGORITHM);
        sg.initVerify(pubKey);
        Log.i(TAG, "Signature Object Info: ");
        Log.i(TAG, "Algorithm = " + sg.getAlgorithm());
        Log.i(TAG, "Provider = " + sg.getProvider());

        File receivedFile = new File(input);
        int fileSize = (int) receivedFile.length();
        FileInputStream in = new FileInputStream(receivedFile);
        byte[] buff = new byte[fileSize];
        int readBytes = in.read(buff);
        in.close();
        if(readBytes != fileSize){
            throw new Exception("Read failure");
        }

        sg.update(buff);

        boolean ok = sg.verify(sign);
        Log.i(TAG, "Verification result = " + ok);
        return ok;
    }

    public static boolean checkFileSignature(Context context, String filePath, String targetPath)
    {
        boolean valid = false;

        unzip(filePath, targetPath);

        Resources resources = context.getResources();
        try
        {
            String filename = resources.getString(R.string.configFilename);
            String fileXmlExt = resources.getString(R.string.config_xml);
            String fileSigExt = resources.getString(R.string.config_sig);

            PublicKey pubKey = RSAUtils.readPublicKeyFromPemFormat(context);
            byte[] sign = RSAUtils.readSignature(targetPath + filename + fileSigExt);
            valid = RSAUtils.verifySignature(targetPath + filename + fileXmlExt, sign, pubKey);
        } catch (Exception e)
        {
            Log.d(TAG, "File signature verification failed: "+ e.getLocalizedMessage());
        }
        return valid;
    }

    private static void unzip(String filePath, String targetPath)
    {
        File path = new File(targetPath);
        boolean notMkDirs = !path.mkdirs();
        if(notMkDirs && !path.exists()){
            Log.e(TAG, "Couldn't create path: " + targetPath);
        }
        else {
            try {
                FileInputStream fin = new FileInputStream(filePath);
                ZipInputStream zin = new ZipInputStream(fin);
                ZipEntry ze;

                while ((ze = zin.getNextEntry()) != null) {
                    Log.d(TAG, "Unzipping " + ze.getName());

                    if (ze.isDirectory()) {
                        _dirChecker(ze.getName(), targetPath);
                    } else {
                        FileOutputStream fout = new FileOutputStream(targetPath + ze.getName());
                        byte[] buffer = new byte[Utils.BUFFER_SIZE_2_KBYTES];

                        int count;

                        while ((count = zin.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }

                        zin.closeEntry();
                        fout.close();
                    }
                }
                zin.close();
                fin.close();
            } catch (Exception e) {
                Log.e(TAG, "unzip", e);
            }
        }
    }

    private static void _dirChecker(String dir, String location)
    {
        String targetPath = location + dir;
        File f = new File(targetPath);

        if (!f.isDirectory())
        {
            boolean notMkDirs = !f.mkdirs();
            if(notMkDirs && !f.exists()){
                Log.e(TAG, "Couldn't create path: " + targetPath);
            }
        }
    }

}
