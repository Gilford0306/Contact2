package com.example.contact2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Contact> contacts;
    private ContactAdapter adapter;
    private EditText newNameEditText;
    private EditText newNumberEditText;

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_WRITE_CONTACTS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contacts = new ArrayList<>();
        adapter = new ContactAdapter(this, contacts);

        ListView contactsListView = findViewById(R.id.contactsListView);
        contactsListView.setAdapter(adapter);

        newNameEditText = findViewById(R.id.newNameEditText);
        newNumberEditText = findViewById(R.id.newNumberEditText);

        Button addContactButton = findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = newNameEditText.getText().toString();
                String number = newNumberEditText.getText().toString();
                if (!name.isEmpty() && !number.isEmpty()) {
                    addContact(name, number);
                    newNameEditText.setText("");
                    newNumberEditText.setText("");
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            loadContacts();
        }
    }

    private void loadContacts() {
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                Contact contact = new Contact(name, number);
                contacts.add(contact);
            }
            cursor.close();
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
        }
    }

    private void addContact(String name, String number) {
        Contact contact = new Contact(name, number);
        contacts.add(contact);
        adapter.notifyDataSetChanged();
        saveContactToPhone(name, number);
    }

    private void saveContactToPhone(String name, String number) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Toast.makeText(this, "Contact saved to phone", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save contact to phone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Permission denied. Cannot load contacts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ContactAdapter extends ArrayAdapter<Contact> {
        public ContactAdapter(@NonNull Context context, @NonNull ArrayList<Contact> contacts) {
            super(context, 0, contacts);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.contact_item, parent, false);
            }

            Contact currentContact = getItem(position);

            TextView contactTextView = itemView.findViewById(R.id.contactTextView);
            ImageButton deleteButton = itemView.findViewById(R.id.deleteButton);

            contactTextView.setText(currentContact.getName() + " - " + currentContact.getNumber());

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteContact(currentContact);
                }
            });

            return itemView;
        }
    }

    private void deleteContact(Contact contact) {
        try {
            String where = ContactsContract.Data.DISPLAY_NAME + " = ? AND " + ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?";
            String[] args = new String[]{contact.getName(), contact.getNumber()};
            getContentResolver().delete(ContactsContract.Data.CONTENT_URI, where, args);
            Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
            contacts.remove(contact);
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error deleting contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
