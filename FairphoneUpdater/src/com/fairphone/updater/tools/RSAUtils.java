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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
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

    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    public static PublicKey readPublicKeyFormCertificate(Context context, int certificateResourceId) throws IOException, CertificateException
    {
        InputStream in = context.getResources().openRawResource(certificateResourceId);
        byte[] buff = new byte[4000];
        int bytesRead;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((bytesRead = in.read(buff)) != -1)
        {
            out.write(buff, 0, bytesRead);
            Log.i(TAG, "bytes read: " + bytesRead);
        }

        byte[] publicKeyBytes = out.toByteArray();

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(publicKeyBytes));

        PublicKey pubKey = cert.getPublicKey();
        Log.i(TAG, "Public Key Info: ");
        Log.i(TAG, "Algorithm = " + pubKey.getAlgorithm());
        return pubKey;
    }

    public static PublicKey readPublicKeyFromPemFormat(Context context, int publicKeyId) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {

        InputStream in = context.getResources().openRawResource(publicKeyId);
        BufferedReader pemReader = new BufferedReader(new InputStreamReader(in));

        StringBuffer content = new StringBuffer();
        String line = null;
        while ((line = pemReader.readLine()) != null)
        {
            if (line.indexOf("-----BEGIN PUBLIC KEY-----") != -1)
            {
                while ((line = pemReader.readLine()) != null)
                {
                    if (line.indexOf("-----END PUBLIC KEY") != -1)
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

    public static byte[] readSignature(String input) throws IOException
    {
        FileInputStream signStream = new FileInputStream(input);
        byte[] signBytes = new byte[signStream.available()];
        signStream.read(signBytes);
        signStream.close();
        return signBytes;
    }

    public static boolean verifySignature(String input, String algorithm, byte[] sign, PublicKey pubKey) throws Exception
    {
        Signature sg = Signature.getInstance(algorithm);
        sg.initVerify(pubKey);
        Log.i(TAG, "Signature Object Info: ");
        Log.i(TAG, "Algorithm = " + sg.getAlgorithm());
        Log.i(TAG, "Provider = " + sg.getProvider());

        FileInputStream in = new FileInputStream(input);
        byte[] buff = new byte[in.available()];
        in.read(buff);
        in.close();

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

            PublicKey pubKey = RSAUtils.readPublicKeyFromPemFormat(context, R.raw.public_key);
            byte[] sign = RSAUtils.readSignature(targetPath + filename + fileSigExt);
            valid = RSAUtils.verifySignature(targetPath + filename + fileXmlExt, RSAUtils.SIGNATURE_ALGORITHM, sign, pubKey);
        } catch (CertificateException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return valid;
    }

    private static void unzip(String filePath, String targetPath)
    {
        new File(targetPath).mkdirs();
        try
        {
            FileInputStream fin = new FileInputStream(filePath);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze = null;

            while ((ze = zin.getNextEntry()) != null)
            {
                Log.d(TAG, "Unzipping " + ze.getName());

                if (ze.isDirectory())
                {
                    _dirChecker(ze.getName(), targetPath);
                }
                else
                {
                    FileOutputStream fout = new FileOutputStream(targetPath + ze.getName());
                    byte buffer[] = new byte[2048];

                    int count = 0;

                    while ((count = zin.read(buffer)) != -1)
                    {
                        fout.write(buffer, 0, count);
                    }

                    zin.closeEntry();
                    fout.close();
                }
            }
            zin.close();
            fin.close();
        } catch (Exception e)
        {
            Log.e(TAG, "unzip", e);
        }
    }

    private static void _dirChecker(String dir, String location)
    {
        File f = new File(location + dir);

        if (!f.isDirectory())
        {
            f.mkdirs();
        }
    }

}
