package com.fairphone.updater.tools;

import android.util.Xml;

import com.fairphone.updater.data.Store;
import com.fairphone.updater.data.UpdaterData;
import com.fairphone.updater.data.Version;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * A helper class for parsing the XML received from the server into a data structure.
 *
 * @author Maarten Derks
 */
public class XmlParser {

    private static final String ns = null;

    /**
     * Parse the content of the specified file input stream into an UpdaterData object.
     *
     * @param fis A FileInputStream containing the XML to be parsed
     * @return an UpdaterData object
     * @throws XmlPullParserException This exception is thrown to signal XML Pull Parser related faults.
     * @throws IOException Signals a general, I/O-related error.
     */
    public UpdaterData parse(FileInputStream fis) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fis, null);
            parser.nextTag();
            return readUpdaterXml(parser);
        } finally {
            fis.close();
        }
    }

    private UpdaterData readUpdaterXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        UpdaterData updaterData = UpdaterData.getInstance();
        updaterData.resetUpdaterData();

        parser.require(XmlPullParser.START_TAG, ns, "updater");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("releases")) {
                parser.require(XmlPullParser.START_TAG, ns, "releases");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String releaseType = parser.getName();
                    if (releaseType.equals("fairphone")) {
                        parser.require(XmlPullParser.START_TAG, ns, "fairphone");
                        updaterData.setLatestFairphoneVersionNumber(parser.getAttributeValue(ns, "latest"));
                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG) {
                                continue;
                            }
                            String name = parser.getName();
                            if (name.equals("version")) {
                                updaterData.addFairphoneVersion(readVersion(parser, Version.IMAGE_TYPE_FAIRPHONE));
                            } else {
                                skip(parser);
                            }
                        }
                    }

                    if (releaseType.equals("aosp")) {
                        parser.require(XmlPullParser.START_TAG, ns, "aosp");
                        updaterData.setLatestAOSPVersionNumber(parser.getAttributeValue(ns, "latest"));
                        while (parser.next() != XmlPullParser.END_TAG) {
                            if (parser.getEventType() != XmlPullParser.START_TAG) {
                                continue;
                            }
                            String name = parser.getName();
                            if (name.equals("version")) {
                                updaterData.addAOSPVersion(readVersion(parser, Version.IMAGE_TYPE_AOSP));
                            } else {
                                skip(parser);
                            }
                        }
                    }
                }
            }

            if (tagName.equals("stores")) {
                parser.require(XmlPullParser.START_TAG, ns, "stores");
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = parser.getName();
                    if (name.equals("store")) {
                        updaterData.addAppStore(readStore(parser));
                    } else {
                        skip(parser);
                    }
                }
            }
        }
        return updaterData;
    }

    private Version readVersion(XmlPullParser parser, String imageType) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "version");

        Version version = new Version();

        version.setId(parser.getAttributeValue(ns, "number"));
        version.setImageType(imageType);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("name")) {
                version.setName(readName(parser));
            } else if (tagName.equals("build_number")) {
                version.setBuildNumber(readBuildNumber(parser));
            } else if (tagName.equals("android_version")) {
                version.setAndroidVersion(readAndroidVersion(parser));
            } else if (tagName.equals("release_notes")) {
                version.setReleaseNotes(Version.DEFAULT_NOTES_LANG, readReleaseNotes(parser));
            } else if (tagName.equals("release_notes" + "_" + Locale.getDefault().getLanguage())) {
                version.setReleaseNotes(Locale.getDefault().getLanguage(), readLocalizedReleaseNotes(parser));
            } else if (tagName.equals("release_date")) {
                version.setReleaseDate(readReleaseDate(parser));
            } else if (tagName.equals("md5sum")) {
                version.setMd5Sum(readMd5sum(parser));
            } else if(tagName.equals("thumbnail_link")) {
                version.setThumbnailLink(readThumbnailLink(parser));
            } else if (tagName.equals("update_link")) {
                version.setDownloadLink(readUpdateLink(parser));
            } else if (tagName.equals("dependencies")) {
                version.setVersionDependencies(readDependencies(parser));
            } else if (tagName.equals("erase_data_warning")) {
                version.setEraseAllPartitionWarning();
		skip(parser);
            } else {
                skip(parser);
            }
        }
        return version;
    }

    private Store readStore(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "store");

        Store store = new Store();

        store.setId(parser.getAttributeValue(ns, "number"));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("name")) {
                store.setName(readName(parser));
            } else if (tagName.equals("build_number")) {
                store.setBuildNumber(readBuildNumber(parser));
            } else if (tagName.equals("release_notes")) {
                store.setReleaseNotes(Version.DEFAULT_NOTES_LANG, readReleaseNotes(parser));
            } else if (tagName.equals("release_notes" + "_" + Locale.getDefault().getLanguage())) {
                store.setReleaseNotes(Locale.getDefault().getLanguage(), readLocalizedReleaseNotes(parser));
            } else if (tagName.equals("release_date")) {
                store.setReleaseDate(readReleaseDate(parser));
            } else if (tagName.equals("md5sum")) {
                store.setMd5Sum(readMd5sum(parser));
            } else if (tagName.equals("update_link")) {
                store.setDownloadLink(readUpdateLink(parser));
            } else if (tagName.equals("show_disclaimer")) {
               store.setShowDisclaimer();
            } else {
                skip(parser);
            }
        }
        return store;
    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return name;
    }

    private String readBuildNumber(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "build_number");
        String buildNumber = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "build_number");
        return buildNumber;
    }

    private String readAndroidVersion(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "android_version");
        String androidVersion = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "android_version");
        return androidVersion;
    }

    private String readReleaseNotes(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "release_notes");
        String releaseNotes = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "release_notes");
        return releaseNotes;
    }

    private String readLocalizedReleaseNotes(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "release_notes" + "_" + Locale.getDefault().getLanguage());
        String releaseNotes = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "release_notes" + "_" + Locale.getDefault().getLanguage());
        return releaseNotes;
    }

    private String readReleaseDate(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "release_date");
        String releaseDate = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "release_date");
        return releaseDate;
    }

    private String readMd5sum(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "md5sum");
        String md5sum = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "md5sum");
        return md5sum;
    }

    private String readThumbnailLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "thumbnail_link");
        String thumbnailLink = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "thumbnail_link");
        return thumbnailLink;
    }

    private String readUpdateLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "update_link");
        String updateLink = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "update_link");
        return updateLink;
    }

    private String readDependencies(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "dependencies");
        String dependencies = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "dependencies");
        return dependencies;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
