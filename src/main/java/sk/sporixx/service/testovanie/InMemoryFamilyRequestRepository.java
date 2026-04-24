package sk.sporixx.service.testovanie;

import sk.sporixx.model.FamilyRequest;
import sk.sporixx.repository.FamilyRequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InMemoryFamilyRequestRepository implements FamilyRequestRepository {

    private final List<FamilyRequest> requests = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public FamilyRequest save(FamilyRequest request) {
        if (request.getId() == 0) {
            request.setId(idGenerator.incrementAndGet());
        }
        requests.add(request);
        return request;
    }

    @Override
    public Optional<FamilyRequest> findById(int id) {
        return requests.stream()
                .filter(r -> r.getId() == id)
                .findFirst();
    }

    @Override
    public List<FamilyRequest> findPendingByToUserId(int toUserId) {
        return requests.stream()
                .filter(r -> r.getToUserId() == toUserId
                        && FamilyRequest.STATUS_PENDING.equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FamilyRequest> findPendingByFromUserId(int fromUserId) {
        return requests.stream()
                .filter(r -> r.getFromUserId() == fromUserId
                        && FamilyRequest.STATUS_PENDING.equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsPending(int fromUserId, int toUserId) {
        return requests.stream()
                .anyMatch(r -> r.getFromUserId() == fromUserId
                        && r.getToUserId() == toUserId
                        && FamilyRequest.STATUS_PENDING.equals(r.getStatus()));
    }

    @Override
    public void updateStatus(int requestId, String status) {
        requests.stream()
                .filter(r -> r.getId() == requestId)
                .findFirst()
                .ifPresent(r -> r.setStatus(status));
    }

    public List<FamilyRequest> findAll() {
        return new ArrayList<>(requests);
    }
}
