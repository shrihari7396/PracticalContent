import streamlit as st
from student_db import create

create()

st.title("Student management System ...")
st.markdown("""
Wlecom to ***Student Management System.*** built **Streamlit**\n
Use the sidebar to navigate to pages\n
- **Add students**
- **Update students**
- **View Students**
- **Delete Student**
""")



students = []

def create():
    print("Students list created Successfully ...")
    return None

def addStudent( roll, name, email, course):
    student = {
        'roll': roll,
        'name': name,
        'email': email,
        'course': course
    }
    for i in students:
        if i['roll'] == roll:
            print("Student Already Exits !!!")
            return -1
    students.append(student)
    return 0

def viewAllStudents():
    return students.copy()

def updateStudent(roll, name = None, email = None, course = None):
    for i in students:
        if i['roll'] == roll:
            if name:
                i['name'] = name
            if email:
                i['email'] = email
            if course:
                i['course'] = course
            print("Student Added Successfully ...")
            return 0
    
    print(f"Student with Specified roll:{roll} Not found in db ....")
    return -1

def deleteStudent(roll):
    for s in students:
        if s['roll'] == roll:
            students.remove(s)
            print("Student deleted Successfully ...")
            return 0
    print("Student not found !!!")
    return -1


import streamlit as st
from student_db import addStudent

st.title("Add new Student")

with st.form("add_form"):
    roll = st.number_input("Enter roll number of student: ", max_value=100, min_value=1)
    name = st.text_input("Enter name of Student: ")
    email = st.text_input("Enter email of Student: ")
    courseName = st.text_input("Enter Course of Student: ")
    submitted = st.form_submit_button("Add Student in db ...")

if submitted:
    if name.strip() and email.strip() and courseName.strip() and courseName.strip():
        res = addStudent(roll=roll, name=name, email=email, course=courseName)
        if res == -1:
            st.warning("Their is Error During addition of Student in DB ...")
            st.error("Error Occured During Addition of Student ...")
        else:
            st.success("Student Added Successfully ...")
            st.balloons()
            st.snow()

import streamlit as st
from student_db import viewAllStudents

data = viewAllStudents()

if data:
    st.subheader("All Students in DB")
    st.table(data)
else:
    st.warning("DB is Empty !!!")


import streamlit as st
from student_db import deleteStudent

roll = st.number_input("Enter Roll Number to Delete from db: ", max_value=100, min_value=1)

if st.button("Delete Student"):
    res = deleteStudent(roll=roll)
    if res != 0:
        st.warning("Enter Valid roll Number ...")
    else:
        st.success(f"Student with roll: {roll} delete Successfully ...")
        st.snow()


import streamlit as st
from student_db import updateStudent

st.title("Enter Student data to update:- ")
roll = st.number_input('Enter Roll Number of Student: ')
name = st.text_input('Enter Name of Student: ')
email = st.text_input('Enter Email of Student: ')
courseName = st.text_input('Enter Course of Student: ')

if st.button("Update Student data"):
    res = updateStudent(roll=roll, name=name, email=email, course=courseName)
    if res != -1:
        st.success("Student Updated Successfully ...")
        # st.balloons()
        st.snow()
    else:
        st.warning("Their is An error occured During the updating data, For more info See logs of Application ....")
