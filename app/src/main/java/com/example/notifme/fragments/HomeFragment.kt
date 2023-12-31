package com.example.notifme.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notifme.R
import com.example.notifme.databinding.FragmentHomeBinding
import com.example.notifme.utils.ToDoAdapter
import com.example.notifme.utils.ToDoData
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeFragment : Fragment(), AddToDoFragment.DiaglogSaveBtnClickListener,
    ToDoAdapter.ToDoAdapterClicksInterface {

    companion object {
        fun newInstance(): AddToDoFragment {
            return AddToDoFragment()
        }
    }

    private lateinit var auth : FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var navController: NavController
    private lateinit var binding: FragmentHomeBinding
    private var taskPopUp: AddToDoFragment? = null
    private lateinit var adapter: ToDoAdapter
    private lateinit var mList:MutableList<ToDoData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FirebaseApp.initializeApp(requireContext())

        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        getDataFromFirebase()
        registerEvents()

    }

    private fun registerEvents() {
        binding.floatBtnAdd.setOnClickListener {
            if (taskPopUp != null) {
                childFragmentManager.beginTransaction().remove(taskPopUp!!).commit()
            }
            taskPopUp = AddToDoFragment()
            taskPopUp!!.setListener(this)
            taskPopUp!!.show(childFragmentManager, "AddTodoFragment")
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut();
            Toast.makeText(context, "Logout successful!", Toast.LENGTH_SHORT).show()
            navController.navigate(R.id.action_homeFragment_to_signInFragment)
        }
     }


    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference.child("Tasks").child(auth.currentUser?.uid.toString())

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mList = mutableListOf()
        adapter = ToDoAdapter(mList)
        adapter.setListener(this)
        binding.recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mList.clear()
                for (taskSnapshot in snapshot.children) {
                    val taskId = taskSnapshot.key
                    val task = taskSnapshot.child("task").getValue(String::class.java)
                    val dueDate = taskSnapshot.child("dueDate").getValue(String::class.java)

                    if (taskId != null && task != null && dueDate != null) {
                        val concatenatedText = "$task - $dueDate"
                        val toDoTask = ToDoData(taskId, concatenatedText, dueDate)
                        mList.add(toDoTask)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSaveTask(task: String, edtTaskName: EditText, taskDueDate: EditText) {
        Log.d("SaveTask", "Task: $task")

        val dueDate = taskDueDate.text.toString()
        val taskMap = mapOf(
            "task" to task,
            "dueDate" to dueDate
        )

        databaseRef.push().setValue(taskMap).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("SaveTask", "Saved successfully!")
                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("SaveTask", "Error saving task.", it.exception)
                Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
        taskPopUp!!.dismiss()
    }


    override fun onUpdateTask(toDoData: ToDoData, edtTaskName: EditText, edtDueDate: EditText) {
        val taskId = toDoData.taskId
        val updatedTask = edtTaskName.text.toString()
        val updatedDueDate = edtDueDate.text.toString()

        val map = HashMap<String, Any>()
        map["task"] = updatedTask
        map["dueDate"] = updatedDueDate

        databaseRef.child(taskId).updateChildren(map).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
            edtTaskName.text = null
            edtDueDate.text = null
            taskPopUp!!.dismiss()
        }
    }

    override fun onDeleteTaskBtnClicked(toDoData: ToDoData) {
        databaseRef.child(toDoData.taskId).removeValue().addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(context, "Deleted successfully!", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEditTaskBtnCLicked(toDoData: ToDoData) {
        if (taskPopUp != null) {
            childFragmentManager.beginTransaction().remove(taskPopUp!!).commit()
        }
        taskPopUp = AddToDoFragment.newInstance(toDoData.taskId, toDoData.task)
        taskPopUp!!.setListener(this)
        taskPopUp!!.show(childFragmentManager, "AddTodoFragment")
    }
}