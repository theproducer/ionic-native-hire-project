package io.ionic.starter;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.pm.PackageManager;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@NativePlugin(
    requestCodes = { Contacts.GET_ALL_REQUEST, Contacts.GET_BY_ID_REQUEST, Contacts.GET_ALL_BY_SEARCH_REQUEST }
)
public class Contacts extends Plugin {
  static final int GET_ALL_REQUEST = 30033;
  static final int GET_BY_ID_REQUEST = 30034;
  static final int GET_ALL_BY_SEARCH_REQUEST = 30035;

  @PluginMethod()
  public void getAll(PluginCall call) {
    if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
      saveCall(call);
      pluginRequestPermissions(new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, GET_ALL_REQUEST);
      return;
    }

    JSObject result = new JSObject();
    JSArray contacts = this.getAllContacts(null);
    result.put("contacts", contacts);
    call.resolve(result);
  }

  @PluginMethod()
  public void getBySearch(PluginCall call) {
    if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
      saveCall(call);
      pluginRequestPermissions(new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, GET_ALL_BY_SEARCH_REQUEST);
      return;
    }

    String searchTerm = call.getString("searchTerm");
    if (searchTerm == null || searchTerm.isEmpty()) {
      call.reject("a search term is required");
      return;
    }

    JSObject result = new JSObject();
    JSArray contacts = this.getAllContacts(searchTerm);
    result.put("contacts", contacts);
    call.resolve(result);
  }

  @PluginMethod()
  public void getById(PluginCall call) {
    if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
      saveCall(call);
      pluginRequestPermissions(new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, GET_BY_ID_REQUEST);
      return;
    }

    String contactId = call.getString("contactId");
    if (contactId == null || contactId.isEmpty()) {
      call.reject("a contact id is required");
      return;
    }

    JSObject contact = this.getContactById(contactId);
    JSObject result = new JSObject();
    result.put("contact", contact);
    call.resolve(result);
  }

  protected JSObject getContactById(String id) {
    final String[] PROJECTION = new String[]{
      ContactsContract.Data.CONTACT_ID,
      ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
      ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
      ContactsContract.CommonDataKinds.GroupMembership.DISPLAY_NAME,
      ContactsContract.Data.DATA1,
      ContactsContract.Data.MIMETYPE
    };

    ContentResolver resolver = this.getContext().getContentResolver();

    Cursor cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION,
            ContactsContract.Data.CONTACT_ID + " = ?" +
                    " AND (" +
                    ContactsContract.Data.MIMETYPE + " = ?" +
                    " OR " +
                    ContactsContract.Data.MIMETYPE + " = ?" +
                    " OR " +
                    ContactsContract.Data.MIMETYPE + " = ?" +
                    ")"
            ,
            new String[]{
                    id,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            },
            null
    );

    ArrayList<AppContact> allContacts = this.processContactsCursor(cursor, null);
    if (allContacts.get(0) != null) {
      AppContact contact = allContacts.get(0);

      JSObject jsonContact = new JSObject();
      jsonContact.put("id", contact.id);
      jsonContact.put("firstName", contact.firstName);
      jsonContact.put("lastName",  contact.lastName);
      jsonContact.put("phoneNumbers", new JSArray(contact.phoneNumbers));
      jsonContact.put("emailAddresses", new JSArray(contact.emailAddresses));

      return jsonContact;
    }

    return null;
  }

  protected JSArray getAllContacts(String searchTerm) {
    final String[] PROJECTION = new String[]{
      ContactsContract.Data.CONTACT_ID,
      ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
      ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
      ContactsContract.CommonDataKinds.GroupMembership.DISPLAY_NAME,
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

    ArrayList<AppContact> contactList = this.processContactsCursor(cursor, searchTerm);

    JSArray jsonContacts = new JSArray();

    for (AppContact contact : contactList) {
      JSObject jsonContact = new JSObject();
      jsonContact.put("id", contact.id);
      jsonContact.put("firstName", contact.firstName);
      jsonContact.put("lastName",  contact.lastName);
      jsonContact.put("phoneNumbers", new JSArray(contact.phoneNumbers));
      jsonContact.put("emailAddresses", new JSArray(contact.emailAddresses));
      jsonContacts.put(jsonContact);
    }

    return jsonContacts;
  }

  private ArrayList<AppContact> processContactsCursor(Cursor cursor, String searchTerm) {
    LinkedHashMap<Integer, AppContact> contacts = new LinkedHashMap<Integer, AppContact>();
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
          newContact.id = Integer.toString(contactId);
          switch (mimeType) {
            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
              newContact.addEmailAddress(emailOrPhone);
              break;
            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
              newContact.addPhoneNumber(emailOrPhone);
              break;
            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
              newContact.setFirstName(firstName);
              newContact.setLastName(lastName);
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
                updatedContact.setFirstName(firstName);
                updatedContact.setLastName(lastName);
                break;
            }
          }
        }
      }
    } finally {
      cursor.close();
    }

    ArrayList<AppContact> contactsList = new ArrayList<>();
    for (Map.Entry<Integer, AppContact> contact : contacts.entrySet()) {
      if (searchTerm != null) {
        boolean found = false;
        if (contact.getValue().firstName.contains(searchTerm) || contact.getValue().lastName.contains(searchTerm)) {
          found = true;
        }

        if (!found) {
          for (String email : contact.getValue().emailAddresses) {
            if (email.contains(searchTerm)) {
              found = true;
              break;
            }
          }

          for (String phone : contact.getValue().phoneNumbers) {
            if (phone.contains(searchTerm)) {
              found = true;
              break;
            }
          }
        }

        if (found) {
          contactsList.add(contact.getValue());
        }
      } else {
        contactsList.add(contact.getValue());
      }
    }

    return contactsList;
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
        savedCall.reject("User denied permissions");
        return;
      }
    }

    if (requestCode == GET_ALL_REQUEST) {
      this.getAll(savedCall);
    }

    if (requestCode == GET_BY_ID_REQUEST) {
      this.getById(savedCall);
    }

    if (requestCode == GET_ALL_BY_SEARCH_REQUEST) {
      this.getBySearch(savedCall);
    }
  }
}
