###Fairphone Updater overview

The Fairphone updater is a built in updater service that provides functionality for OTA - Over The Air updates of the Fairphone OS.

**Query parameters available:** 

* model    --- Fairphone model.
* os          --- Android version.
* b_n        --- Current image build number.
* ota_v_n --- Current image version number.
* d            --- Current image build date in UTC unix time.
* beta       --- Beta program status. 0 is beta disabled; 1 is beta enabled.
* dev        --- Developer mode status. 0 is dev mode disabled; 1 is dev mode enabled.
 
Ex: http://www.server.com/updater.zip?model=FP1U&os=4.2.2&b_n=1.8&ota_v_n=6&d=1416798101&beta=1&dev=0

**Updater config file elements:**


```
#!xml

<?xml version="1.0" encoding="utf-8"?>
<updater>
  <releases> ---------> This element will have all the versions available.
    <aosp latest="2"> ---------> This element will have all the stock android versions available. The "latest" attribute will have the version number of the latest version. The version corresponding to the latest id must appear in the versions list below.
      <version number="1"> ---------> This element will have all the information about a version. The "number" attribute will have the version number.
        <name>Version Name</name> ---------> This element will have the version name.
        <build_number>Version Build Number</build_number> ---------> This element will have the version build number.
        <android_version>Version Android </android_version> ---------> This element will have the version android version.
        <release_notes>Release notes default</release_notes> ---------> This element will have the version release notes.
        <release_notes_fr>Release notes FR</release_notes_fr> ---------> This element will have the version release notes in a given language. Add _country_code (Ex: _fr for France) to the release_notes tag (Ex: release_notes_fr). These tags are optional. 
        <release_date>05/08/2014</release_date> ---------> This element will have the version release date. This tag is optional. 
        <md5sum>ff7a312ae9fbc52d591fcd4b213b9321</md5sum> ---------> This element will have the update file MD5 checksum.
        <thumbnail_link>Version logo image</thumbnail_link> ---------> This element will have the version logo thumbnail download link. This tag is optional. 
        <update_link>Update Url</update_link> ---------> This element will have the version OTA image download link.
        <erase_data_warning/> ---------> This element indicates that if this update is performed all the user data will be erased. This tag is optional. 
      </version>
      <version number="2">
        <name>Jelly Bean</name>
        (...)
      </version>
    </aosp>
    <fairphone latest="2"> ---------> This element will have all the fairphone versions available. The "latest" attribute will have the version number of the latest version. The version corresponding to the latest id must appear in the versions list below.
      <version number="1"> ---------> This element will have all the information about a version. The "number" attribute will have the version number.
    		(...) ---------> Will have the same information described above in the AOSP versions.
    	</version>
      <version number="2">
        (...)
      </version>
    </fairphone>
  </releases>
  <stores> ---------> This element will have all the stores available. This group is optional.
    <store number="0"> ---------> This element will have all the information about a store. The "number" attribute will have the store number Id. Number 0 is reserved for Google Apps.
      <name>Google Apps</name> ---------> This element will have the store name.
      <build_number>1.0</build_number> ---------> This element will have the store build number.
      <release_notes>Release notes default</release_notes> ---------> This element will have the store release notes.
      <release_notes_fr>Release notes FR</release_notes_fr> ---------> This element will have the store release notes in a given language. Add _country_code (Ex: _fr for France) to the release_notes tag (Ex: release_notes_fr). These tags are optional.
      <release_date>20/12/2013</release_date> ---------> This element will have the store release date. This tag is optional.
      <md5sum>655911fa2dbac0ed327cda53431ce2d6</md5sum> ---------> This element will have the update file MD5 checksum.
      <update_link>Store package URL</update_link> ---------> This element will have the store OTA image download link.
      <show_disclaimer/> ---------> This element is used to show the gapps disclamer prior to install. This tag is optional.
    </store>
    <store number="1">
      (...) ---------> Will have the same information described above.
    </store>
  </stores>
</updater>
```

**Updater server path structure:**

```
Root -> Server URL
      |
      |--> latest.zip -> Old updater config file
      |
      |--> FP1/   -> FP1 original and FP1 Fuse fit here since they have the same Model
      |    |--> originalPartition/   -> FP1 with 1GB data partition 
      |    |    |--> updater.zip
      |    |--> unifiedPartition/    -> FP1 with unified data partition
      |         |--> updater.zip
      |--> FP1U
      |    |--> updater.zip
      |--> FP2
      |    |--> updater.zip
      (...)
```