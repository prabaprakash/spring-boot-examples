package com.in28minutes.springboot.rest.example.student;

import java.net.URI;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class StudentResource {

	@Autowired
	private StudentRepository studentRepository;

	private int attempts = 0;
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	@GetMapping("/students")
	@Retryable(
			value = { SQLException.class },
			maxAttempts = 4,
			backoff = @Backoff(delay = 5000))
	public List<Student> retrieveAllStudents() throws SQLException {
		System.out.println("Calculating - Attempt " + attempts + " at " + sdf.format(new Date()));
		attempts++;
		try {
			 if(attempts == 2){
			 	return studentRepository.findAll();
			 }
			 else{
			 	throw new SQLException("throwned by me");
			 }
		}
		catch (SQLException e) {
			throw new SQLException(e);
		}
	}
	@Recover
	public List<Student> recover(SQLException e){
		System.out.println("Recovering - returning safe value");
		return studentRepository.findAll();
	}
	@GetMapping("/students/{id}")
	public Student retrieveStudent(@PathVariable long id) {
		Optional<Student> student = studentRepository.findById(id);

		if (!student.isPresent())
			throw new StudentNotFoundException("id-" + id);

		return student.get();
	}

	@DeleteMapping("/students/{id}")
	public void deleteStudent(@PathVariable long id) {
		studentRepository.deleteById(id);
	}

	@PostMapping("/students")
	public ResponseEntity<Object> createStudent(@RequestBody Student student) {
		Student savedStudent = studentRepository.save(student);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(savedStudent.getId()).toUri();

		return ResponseEntity.created(location).build();

	}
	
	@PutMapping("/students/{id}")
	public ResponseEntity<Object> updateStudent(@RequestBody Student student, @PathVariable long id) {

		Optional<Student> studentOptional = studentRepository.findById(id);

		if (!studentOptional.isPresent())
			return ResponseEntity.notFound().build();

		student.setId(id);
		
		studentRepository.save(student);

		return ResponseEntity.noContent().build();
	}
}
