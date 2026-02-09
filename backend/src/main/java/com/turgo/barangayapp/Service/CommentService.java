package com.turgo.barangayapp.Service;

import com.turgo.barangayapp.Model.Comment;
import com.turgo.barangayapp.Model.User;
import com.turgo.barangayapp.Repository.CommentRepository;
import com.turgo.barangayapp.dtos.FilterComment; // Your new DTO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    // A basic list of bad words (You can expand this later or load from DB)
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

    public Comment addComment(FilterComment request, User user) throws ProfanityWarningException {
        // 1. Check for bad words
        boolean hasProfanity = containsProfanity(request.getContent());

        // 2. Logic: If bad words exist AND user hasn't said "Yes, I'm sure" (confirmed)
        if (hasProfanity && !request.isConfirmed()) {
            throw new ProfanityWarningException("This comment contains inappropriate language. Are you sure you want to post it?");
        }

        // 3. Save the comment
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setContent(request.getContent());
        comment.setEntityType(request.getEntityType().toUpperCase());
        comment.setEntityId(request.getEntityId());
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    private boolean containsProfanity(String text) {
        if (text == null) return false;
        String lowerCaseText = text.toLowerCase();
        // Simple check: does the text contain any of the bad words?
        return BAD_WORDS.stream().anyMatch(lowerCaseText::contains);
    }

    // Custom Exception for the Warning
    public static class ProfanityWarningException extends Exception {
        public ProfanityWarningException(String message) {
            super(message);
        }
    }
}
