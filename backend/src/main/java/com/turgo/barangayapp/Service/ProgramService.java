package com.turgo.barangayapp.Service;

import com.turgo.barangayapp.Model.Program;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.ProgramRepository;
import com.turgo.barangayapp.Service.UserServices; // Needed for removing participants
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProgramService {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private UserServices userServices;

    // --- READ ---
    public List<Program> getAllActivePrograms() {
        return programRepository.findByEndDateAfterOrderByStartDateAsc(LocalDateTime.now());
    }

    public Optional<Program> getProgramById(Long id) {
        return programRepository.findById(id);
    }

    // --- CREATE ---
    public Program createProgram(Map<String, String> request) {
        Program program = new Program();
        program.setName(request.get("name"));
        program.setDescription(request.get("description"));
        program.setStartDate(LocalDateTime.parse(request.get("startDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        program.setEndDate(LocalDateTime.parse(request.get("endDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        program.setActive(true);

        if (request.containsKey("imageUrl")) {
            program.setImageUrl(request.get("imageUrl"));
        }
        return programRepository.save(program);
    }

    // --- UPDATE ---
    public Optional<Program> updateProgram(Long id, Map<String, String> request) {
        return programRepository.findById(id).map(program -> {
            if (request.containsKey("name")) program.setName(request.get("name"));
            if (request.containsKey("description")) program.setDescription(request.get("description"));
            if (request.containsKey("startDate")) program.setStartDate(LocalDateTime.parse(request.get("startDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (request.containsKey("endDate")) program.setEndDate(LocalDateTime.parse(request.get("endDate"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            if (request.containsKey("isActive")) program.setActive(Boolean.parseBoolean(request.get("isActive")));
            if (request.containsKey("imageUrl")) program.setImageUrl(request.get("imageUrl"));

            return programRepository.save(program);
        });
    }

    // --- DELETE ---
    public boolean deleteProgram(Long id) {
        if (programRepository.existsById(id)) {
            programRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // --- PARTICIPATION LOGIC ---

    // Returns a String message if error, or "SUCCESS" if okay
    public String joinProgram(Long programId, User user) {
        Optional<Program> programOpt = programRepository.findById(programId);

        if (programOpt.isEmpty()) return "Program not found";
        Program program = programOpt.get();

        // Check if program is active/future
        if (program.getEndDate().isBefore(LocalDateTime.now()) || !program.isActive()) {
            return "Cannot join: Program has ended or is inactive";
        }

        if (program.getParticipants().contains(user)) {
            return "Already joined this program";
        }

        program.getParticipants().add(user);
        programRepository.save(program);
        return "SUCCESS";
    }

    public String leaveProgram(Long programId, User user) {
        Optional<Program> programOpt = programRepository.findById(programId);
        if (programOpt.isEmpty()) return "Program not found";

        Program program = programOpt.get();
        if (!program.getParticipants().contains(user)) {
            return "Not a participant of this program";
        }

        program.getParticipants().remove(user);
        programRepository.save(program);
        return "SUCCESS";
    }

    public String removeParticipant(Long programId, Long userId) {
        Optional<Program> programOpt = programRepository.findById(programId);
        Optional<User> userOpt = userServices.findById(userId);

        if (programOpt.isEmpty() || userOpt.isEmpty()) {
            return "Program or user not found";
        }

        Program program = programOpt.get();
        User user = userOpt.get();

        if (program.getParticipants().remove(user)) {
            programRepository.save(program);
            return "SUCCESS";
        } else {
            return "User was not a participant";
        }
    }
}