package io.ionic.starter;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.pm.PackageManager;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@NativePlugin(
    requestCodes = { Contacts.GET_ALL_REQUEST }
)
public class Contacts extends Plugin {
  static final int GET_ALL_REQUEST = 30033;

  @PluginMethod()
  public void getAll(PluginCall call) {
    if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
      saveCall(call);
      pluginRequestPermissions(new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, GET_ALL_REQUEST);
      return;
    }

    JSObject result = new JSObject();
    JSArray contacts = this.getAllContacts();
    result.put("contacts", contacts);
    call.success(result);
  }

  protected JSArray getAllContacts() {
    LinkedHashMap<Integer, AppContact> contacts = new LinkedHashMap<Integer, AppContact>();

    final String[] PROJECTION = new String[]{
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.MIMETYPE
    };

    ContentResolver resolver = this.getContext().getContentResolver();

    Cursor cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION,
            ContactsContract.Data.MIMETYPE + " = ?" +
                    " OR " +
                    ContactsContract.Data.MIMETYPE + " = ?" +
                    " OR " +
                    ContactsContract.Data.MIMETYPE + " = ?"
            ,
            new String[]{
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            },
            null
    );

    try {
      final int idPos = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
      final int firstNamePos = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
      final int lastNamePos = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
      final int mimeTypePos = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
      final int emailPhonePos = cursor.getColumnIndex(ContactsContract.Data.DATA1);

      while (cursor.moveToNext()) {
        int contactId = cursor.getInt(idPos);
        String firstName = cursor.getString(firstNamePos);
        String lastName = cursor.getString(lastNamePos);
        String mimeType = cursor.getString(mimeTypePos);
        String emailOrPhone = cursor.getString(emailPhonePos);

        if (contacts.get(contactId) == null) {
          AppContact newContact = new AppContact();
          switch (mimeType) {
            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
              newContact.addEmailAddress(emailOrPhone);
              break;
            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
              newContact.addPhoneNumber(emailOrPhone);
              break;
            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
              newContact.firstName = firstName;
              newContact.lastName = lastName;
              break;
          }
          contacts.put(contactId, newContact);
        } else {
          AppContact updatedContact = contacts.get(contactId);
          if (updatedContact != null) {
            switch (mimeType) {
              case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                updatedContact.addEmailAddress(emailOrPhone);
                break;
              case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                updatedContact.addPhoneNumber(emailOrPhone);
                break;
              case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                updatedContact.firstName = firstName;
                updatedContact.lastName = lastName;
                break;
            }
          }
        }
      }
    } finally {
      cursor.close();
    }


    JSArray jsonContacts = new JSArray();
    for (Map.Entry<Integer, AppContact> contact : contacts.entrySet()) {
      JSObject jsonContact = new JSObject();
      jsonContact.put("firstName", contact.getValue().firstName);
      jsonContact.put("lastName",  contact.getValue().lastName);
      jsonContact.put("phoneNumbers", new JSArray(contact.getValue().phoneNumbers));
      jsonContact.put("emailAddresses", new JSArray(contact.getValue().emailAddresses));
      jsonContacts.put(jsonContact);
    }

    return jsonContacts;
  }

  @Override
  protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

    PluginCall savedCall = getSavedCall();
    if (savedCall == null) {
      return;
    }

    for(int result : grantResults) {
      if (result == PackageManager.PERMISSION_DENIED) {
        savedCall.error("User denied permission");
        return;
      }
    }

    if (requestCode == GET_ALL_REQUEST) {
      this.getAll(savedCall);
    }
  }
}
