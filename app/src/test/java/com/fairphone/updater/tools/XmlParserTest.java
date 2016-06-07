package com.fairphone.updater.tools;

import com.fairphone.updater.BuildConfig;
import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class XmlParserTest {

    private XmlParser xmlParser;

    private static File getFileFromPath(Object obj, String fileName) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        return new File(resource.getPath());
    }

    private static FileInputStream getFileInputStreamFromFile(Object obj, String filename) {
        File file = getFileFromPath(obj, filename);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fis;
    }

    @Before
    public void setUp() {
        xmlParser = new XmlParser();
    }

    @Test
    public void latestFp1Xml() {
        FileInputStream fis = getFileInputStreamFromFile(this, "fp1_fpos_latest.xml");
        UpdaterData updaterData = null;
        try {
            updaterData = xmlParser.parse(fis);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(updaterData);

        assertNotNull(updaterData.getFairphoneVersionList());
        assertThat(updaterData.isFairphoneVersionListNotEmpty(), is(true));
        assertThat(updaterData.getFairphoneVersionList().size(), is(6));
        assertNotNull(updaterData.getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE));

        assertNotNull(updaterData.getAOSPVersionList());
        assertThat(updaterData.isAOSPVersionListNotEmpty(), is(true));
        assertThat(updaterData.getAOSPVersionList().size(), is(1));
        assertNotNull(updaterData.getLatestVersion(Version.IMAGE_TYPE_AOSP));
        Version latest = updaterData.getLatestVersion(Version.IMAGE_TYPE_AOSP);
        assertThat(latest.getImageType(), is(Version.IMAGE_TYPE_AOSP));
        assertThat(latest.getName(), is("Jelly Bean 11-2014"));
        assertThat(latest.getBuildNumber(), is("4.2.2"));
        assertThat(latest.getMd5Sum(), is("20286cb405e1487066c31b55438311ac"));
        assertThat(latest.getDownloadLink(), is("http://storage.googleapis.com/update-v1_8/AOSP-4.2.2_OTA_2014-11-26.zip"));
        assertThat(latest.hasEraseAllPartitionWarning(), is(false));

        assertNotNull(updaterData.getAppStoreList());
        assertThat(updaterData.isAppStoreListEmpty(), is(false));
        assertThat(updaterData.getAppStoreList().size(), is(1));

        Store store = updaterData.getStore("0");
        assertThat(store.getId(), is("0"));
        assertThat(store.getName(), is("Google Apps"));
        assertThat(store.getBuildNumber(), is("1.0"));
        assertThat(store.getReleaseNotes(Version.DEFAULT_NOTES_LANG), is("Google Apps package"));
        assertThat(store.getMd5Sum(), is("9444e40ed5afc9d61530681542f97b97"));
        assertThat(store.getDownloadLink(), is("http://s3-eu-west-1.amazonaws.com/gatest15/gapps_1.5.zip"));
        assertThat(store.showDisclaimer(), is(true));
    }

    @Test
    public void latestFp2Xml() {
        FileInputStream fis = getFileInputStreamFromFile(this, "fp2_fpos_latest.xml");
        UpdaterData updaterData = null;
        try {
            updaterData = xmlParser.parse(fis);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(updaterData);

        assertNotNull(updaterData.getFairphoneVersionList());
        assertThat(updaterData.isFairphoneVersionListNotEmpty(), is(true));
        assertThat(updaterData.getFairphoneVersionList().size(), is(1));
        assertNotNull(updaterData.getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE));
        Version latest = updaterData.getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE);
        assertThat(latest.getImageType(), is(Version.IMAGE_TYPE_FAIRPHONE));
        assertThat(latest.getName(), is("Fairphone OS"));
        assertThat(latest.getBuildNumber(), is("1.4.2"));
        assertThat(latest.getMd5Sum(), is("f0d157ef40fc0bfa4bdb3911e772a4c0"));
        assertThat(latest.getDownloadLink(), is("http://storage.googleapis.com/fairphone-updates/FP2-gms56-1.4.2-ota.zip"));
        assertThat(latest.hasEraseAllPartitionWarning(), is(false));

        assertNotNull(updaterData.getAOSPVersionList());
        assertThat(updaterData.isAOSPVersionListNotEmpty(), is(false));
        assertThat(updaterData.getAOSPVersionList().size(), is(0));
        assertNull(updaterData.getLatestVersion(Version.IMAGE_TYPE_AOSP));

        assertNotNull(updaterData.getAppStoreList());
        assertThat(updaterData.isAppStoreListEmpty(), is(true));
        assertThat(updaterData.getAppStoreList().size(), is(0));
    }

    @Test
    public void latestFp2OpenXml() {
        FileInputStream fis = getFileInputStreamFromFile(this, "fp2_fpopen_latest.xml");
        UpdaterData updaterData = null;
        try {
            updaterData = xmlParser.parse(fis);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(updaterData);

        assertNotNull(updaterData.getFairphoneVersionList());
        assertThat(updaterData.isFairphoneVersionListNotEmpty(), is(true));
        assertThat(updaterData.getFairphoneVersionList().size(), is(2));
        assertNotNull(updaterData.getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE));
        Version latest = updaterData.getLatestVersion(Version.IMAGE_TYPE_FAIRPHONE);
        assertThat(latest.getImageType(), is(Version.IMAGE_TYPE_FAIRPHONE));
        assertThat(latest.getName(), is("Fairphone Open Source OS"));
        assertThat(latest.getBuildNumber(), is("16.05.0"));
        assertThat(latest.getMd5Sum(), is("ad533205938163686aaed40a75073fcd"));
        assertThat(latest.getDownloadLink(), is("http://storage.googleapis.com/fairphone-updates/fp2-sibon-16.05.0-ota-userdebug.zip"));
        assertThat(latest.hasEraseAllPartitionWarning(), is(false));

        assertNotNull(updaterData.getAOSPVersionList());
        assertThat(updaterData.isAOSPVersionListNotEmpty(), is(false));
        assertThat(updaterData.getAOSPVersionList().size(), is(0));
        assertNull(updaterData.getLatestVersion(Version.IMAGE_TYPE_AOSP));

        assertNotNull(updaterData.getAppStoreList());
        assertThat(updaterData.isAppStoreListEmpty(), is(true));
        assertThat(updaterData.getAppStoreList().size(), is(0));
    }
}
