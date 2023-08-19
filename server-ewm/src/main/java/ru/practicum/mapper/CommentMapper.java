package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.RequestCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

import java.time.LocalDateTime;

@Component
public class CommentMapper {
    public Comment toComment(RequestCommentDto commentDto, Event event, User user) {
        return Comment.builder()
                .created(LocalDateTime.now())
                .author(user)
                .event(event)
                .text(commentDto.getText())
                .build();
    }

    public CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .created(comment.getCreated())
                .authorName(comment.getAuthor().getName())
                .text(comment.getText())
                .id(comment.getId())
                .build();
    }
}
