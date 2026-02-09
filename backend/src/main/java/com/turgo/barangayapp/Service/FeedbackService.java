package com.turgo.barangayapp.Service;

import com.turgo.barangayapp.Model.Feedback;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    // Same Profanity List as Comments
    private static final List<String> BAD_WORDS = Arrays.asList(
            // --- English Common ---
            "fuck", "shit", "bitch", "asshole", "dick", "pussy", "cunt", "cock",
            "bastard", "slut", "whore", "damn", "crap", "piss", "nigger", "nigga", "fag", "faggot",

            // --- Filipino Common ---
            "puta", "putangina", "gago", "tanga", "bobo", "ulol", "tarantado",
            "kupal", "kantot", "pekpek", "tite", "etits", "burat", "puke", "keps",
            "hindot", "punyeta", "inutil", "buwisit", "leche", "pakshet", "ogag",
            "ungas", "siraulo", "buang", "hudas", "animal",

            // --- Obfuscated / Leet Speak (English) ---
            "f*ck", "fck", "fvck", "fuc", "fuhck", "fucc",
            "sh*t", "sh!t", "sh1t", "s*it", "shiit",
            "b*tch", "b!tch", "biatch", "b1tch",
            "a$$", "a$$hole", "assh0le", "@ss",
            "d*ck", "d1ck", "d!ck",
            "p*ssy", "puss", "pucy",

            // --- Obfuscated / Leet Speak (Filipino) ---
            "put@", "put4", "ptangina", "tangina", "t@ngina", "tngina", "pota", "potangina",
            "g@go", "g4go", "gag0",
            "t@nga", "t4nga", "tang@",
            "b0bo", "b0b0", "bob0",
            "ul0l", "vlol",
            "kntot", "k@ntot",
            "kup@l", "kup4l",
            "h1ndot", "hind0t"
    );

    // --- CREATE ---
    public Feedback submitFeedback(Map<String, String> request, User user) {
        String message = request.get("message");

        if (message == null || message.trim().length() < 10) {
            throw new IllegalArgumentException("Feedback must be at least 10 characters");
        }

        // Profanity Check (Hard Block)
        if (containsProfanity(message)) {
            throw new IllegalArgumentException("PROFANITY_DETECTED");
        }

        Feedback feedback = new Feedback();
        feedback.setMessage(message.trim());
        feedback.setUser(user);
        return feedbackRepository.save(feedback);
    }

    // --- READ ---
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderBySubmittedAtDesc();
    }

    // --- REPLY (ADMIN) ---
    public Feedback replyToFeedback(Long id, String reply) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(id);
        if (feedbackOpt.isEmpty()) {
            throw new IllegalArgumentException("Feedback not found");
        }

        Feedback feedback = feedbackOpt.get();
        feedback.setAdminReply(reply.trim());
        feedback.setRepliedAt(LocalDateTime.now());
        feedback.setRead(true);

        return feedbackRepository.save(feedback);
    }

    // --- DELETE (ADMIN) ---
    public boolean deleteFeedback(Long id) {
        if (feedbackRepository.existsById(id)) {
            feedbackRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Helper Method
    private boolean containsProfanity(String text) {
        if (text == null) return false;
        String lowerCaseText = text.toLowerCase();
        return BAD_WORDS.stream().anyMatch(lowerCaseText::contains);
    }
}
