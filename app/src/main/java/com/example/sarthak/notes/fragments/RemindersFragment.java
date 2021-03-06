package com.example.sarthak.notes.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sarthak.notes.R;
import com.example.sarthak.notes.activities.HomeScreenActivity;
import com.example.sarthak.notes.activities.NotesActivity;
import com.example.sarthak.notes.adapters.RemindersFragmentRecyclerAdapter;
import com.example.sarthak.notes.firebasemanager.FirebaseAuthorisation;
import com.example.sarthak.notes.models.ChecklistReminders;
import com.example.sarthak.notes.models.NoteReminders;
import com.example.sarthak.notes.models.Notes;
import com.example.sarthak.notes.utils.Constants;
import com.example.sarthak.notes.utils.RemindersRecyclerViewItemClickListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class RemindersFragment extends Fragment implements RemindersRecyclerViewItemClickListener {

    ArrayList<Object> remindersList = new ArrayList<>();
    ArrayList<String> typeOfNote = new ArrayList<>();

    private ProgressDialog progressDialog;

    private RemindersFragmentRecyclerAdapter remindersFragmentRecyclerAdapter;

    DatabaseReference mDatabase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminders, container, false);

        // set title bar
        ((HomeScreenActivity) getActivity()).getSupportActionBar().setTitle(R.string.reminders);

        // set up an instance of firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        // set up progress dialog
        setUpProgressDialog();

        // read 'Reminders' data from firebase database
        readRemindersFromFirebase();

        RecyclerView mRemindersList = (RecyclerView) view.findViewById(R.id.remindersList);
        remindersFragmentRecyclerAdapter = new RemindersFragmentRecyclerAdapter(getActivity(), remindersList, typeOfNote);
        remindersFragmentRecyclerAdapter.setOnRecyclerViewItemClickListener(this);

        // set grid layout with 2 columns
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 2);
        mRemindersList.setLayoutManager(mLayoutManager);
        mRemindersList.setItemAnimator(new DefaultItemAnimator());
        mRemindersList.setAdapter(remindersFragmentRecyclerAdapter);

        return view;
    }

    //----------------------------------------------------------------------------------------------
    // Callback to recyclerView item click from RemindersFragmentRecyclerAdapter
    //----------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view, int position) {

        // launch Notes Activity
        Intent notesIntent = new Intent(getActivity(), NotesActivity.class);
        notesIntent.putExtra(Constants.INTENT_PASS_POSITION, position + 1);

        if (typeOfNote.get(position).equals(Constants.TYPE_NOTES)) {

            // launch 'TakeNotes' or 'TakeChecklists' fragment based on value of notesType
            notesIntent.putExtra(Constants.INTENT_PASS_NOTES_TYPE, Constants.INTENT_PASS_NOTE_REMINDERS);
            notesIntent.putExtra(Constants.INTENT_PASS_SERIALIZABLE_OBJECT, (NoteReminders) remindersList.get(position));
        }
        else if (typeOfNote.get(position).equals(Constants.TYPE_CHECKLISTS)) {

            notesIntent.putExtra(Constants.INTENT_PASS_NOTES_TYPE, Constants.INTENT_PASS_CHECKLIST_REMINDERS);
            notesIntent.putExtra(Constants.INTENT_PASS_SERIALIZABLE_OBJECT, (ChecklistReminders) remindersList.get(position));
        }
        startActivity(notesIntent);
    }

    @Override
    public void onLongClick(View view, int position) {

        // remove item from firebase database
        removeRemindersFromList(position);
    }

    /**
     * Set up progress dialog
     */
    private void setUpProgressDialog() {

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.fetch_data_dialog_message));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    /**
     * Read firebase database and store values of notes as an arraylist object.
     *
     * Since notesBody is a key that will be specified only for Notes and not for Checklists,
     * a check for the same is made and data is added to remindersList as 'NoteReminders' if
     * notesBody is not null and as 'ChecklistReminders' if it is null.
     *
     * To maintain a track of the type of note that is added to notesList, a string value
     * specifying the type of note is added to 'typeOfNote'.
     */
    public void readRemindersFromFirebase() {

        FirebaseAuthorisation firebaseAuthorisation = new FirebaseAuthorisation(getActivity());
        String currentUser = firebaseAuthorisation.getCurrentUser();

        DatabaseReference notesDatabase;
        notesDatabase = mDatabase.child(currentUser).child(Constants.TYPE_REMINDERS);

        if (notesDatabase != null) {

            notesDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    // clear notesList to remove any redundant data
                    remindersList.clear();
                    typeOfNote.clear();

                    for ( DataSnapshot userDataSnapshot : dataSnapshot.getChildren() ) {

                        if (userDataSnapshot != null) {

                            if (userDataSnapshot.getValue(Notes.class).getNotesBody() != null) {
                                // add values fetched from firebase database to 'NoteReminders' notesList
                                remindersList.add(userDataSnapshot.getValue(NoteReminders.class));
                                typeOfNote.add(Constants.TYPE_NOTES);
                            } else {
                                // add values fetched from firebase database to 'ChecklistReminders' notesList
                                remindersList.add(userDataSnapshot.getValue(ChecklistReminders.class));
                                typeOfNote.add(Constants.TYPE_CHECKLISTS);
                            }
                            // update recycler view adapter
                            remindersFragmentRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                    // dismiss progress dialog
                    progressDialog.dismiss();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // update recycler view adapter
                    remindersFragmentRecyclerAdapter.notifyDataSetChanged();
                }
            });
        } else {
            // dismiss progress dialog
            progressDialog.dismiss();
        }
    }

    /**
     * Creates an alert dialog to confirm user to remove Note.
     *
     * @param position is the index of the Note in the recyclerView
     */
    private void removeRemindersFromList(final int position) {

        // setup alert dialog
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_note_alert_message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        removeReminders(position);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


    /**
     * Removes Note at specified from position from firebase database.
     * Updates index of notes following the deleted item in the firebase database.
     *
     * @param position is the position of the item to be deleted in firebase database
     */
    private void removeReminders(int position) {

        final int notePosition = position + 1;

        // get firebase current user
        String currentUser = new FirebaseAuthorisation(getActivity()).getCurrentUser();

        DatabaseReference removeDataReference = mDatabase.child(currentUser).child(Constants.TYPE_REMINDERS)
                .child(Constants.TYPE_REMINDERS + "_0" + String.valueOf(notePosition));
        removeDataReference.removeValue();

        //-------------------------------------------------------------------------------------
        // update index of news items following deleted news item
        //-------------------------------------------------------------------------------------
        // decrease index of following news items by 1.
        for (int i = notePosition + 1 ; i <= remindersList.size() ; i++) {

            DatabaseReference remindersReference = mDatabase.child(currentUser)
                    .child(Constants.TYPE_REMINDERS).child(Constants.TYPE_REMINDERS + "_0" + String.valueOf(i - 1));

            Object remindersItem = remindersList.get(i - 1);

            remindersReference.setValue(remindersItem);
        }

        // remove last item in firebase database as it has been copied to its previous location
        DatabaseReference removeDatabaseFinalValueReference = mDatabase.child(currentUser)
                .child(Constants.TYPE_REMINDERS).child(Constants.TYPE_REMINDERS + "_0" + String.valueOf(remindersList.size()));
        removeDatabaseFinalValueReference.removeValue();
    }
}
