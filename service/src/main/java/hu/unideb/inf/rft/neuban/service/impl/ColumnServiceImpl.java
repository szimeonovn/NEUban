package hu.unideb.inf.rft.neuban.service.impl;

import com.google.common.collect.Lists;
import hu.unideb.inf.rft.neuban.persistence.entities.ColumnEntity;
import hu.unideb.inf.rft.neuban.persistence.repositories.ColumnRepository;
import hu.unideb.inf.rft.neuban.service.domain.BoardDto;
import hu.unideb.inf.rft.neuban.service.domain.ColumnDto;
import hu.unideb.inf.rft.neuban.service.exceptions.BoardNotFoundException;
import hu.unideb.inf.rft.neuban.service.exceptions.ColumnAlreadyExistsException;
import hu.unideb.inf.rft.neuban.service.exceptions.ColumnNotFoundException;
import hu.unideb.inf.rft.neuban.service.interfaces.BoardService;
import hu.unideb.inf.rft.neuban.service.interfaces.ColumnService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Service
public class ColumnServiceImpl implements ColumnService {

    @Autowired
    private BoardService boardService;
    @Autowired
    private ColumnRepository columnRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional(readOnly = true)
    @Override
    public Optional<ColumnDto> get(final Long columnId) {
        Assert.notNull(columnId);

        final Optional<ColumnEntity> columnEntityOptional = Optional.ofNullable(this.columnRepository.findOne(columnId));

        if (columnEntityOptional.isPresent()) {
            return Optional.of(this.modelMapper.map(columnEntityOptional.get(), ColumnDto.class));
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ColumnDto> getAllByBoardId(final Long boardId) {
        final Optional<BoardDto> boardDtoOptional = this.boardService.get(boardId);

        if (boardDtoOptional.isPresent()) {
            return boardDtoOptional.get().getColumns();
        }
        return Lists.newArrayList();
    }

    @Transactional
    @Override
    public void save(final Long boardId, final ColumnDto columnDto) throws BoardNotFoundException, ColumnAlreadyExistsException {
        Assert.notNull(columnDto);
        final BoardDto boardDto = this.boardService.get(boardId).orElseThrow(() -> new BoardNotFoundException(String.valueOf(boardId)));

        if (columnDto.getId() == null || boardDto.getColumns().stream().noneMatch(c -> c.getId().equals(columnDto.getId()))) {
            boardDto.getColumns().add(columnDto);
        } else {
            throw new ColumnAlreadyExistsException(String.valueOf(columnDto.getId()));
        }
        this.boardService.update(boardDto);
    }

    @Transactional
    @Override
    public void update(final ColumnDto columnDto) throws ColumnNotFoundException {
        Assert.notNull(columnDto);

        if (columnDto.getId() == null) {
            throw new ColumnNotFoundException(String.valueOf(columnDto.getId()));
        }

        this.columnRepository.saveAndFlush(this.modelMapper.map(columnDto, ColumnEntity.class));
    }

    @Transactional
    @Override
    public void remove(final Long columnId) throws ColumnNotFoundException {
        Assert.notNull(columnId);

        Optional.ofNullable(this.columnRepository.findOne(columnId))
                .orElseThrow(() -> new ColumnNotFoundException(String.valueOf(columnId)));

        this.columnRepository.delete(columnId);
    }
}
