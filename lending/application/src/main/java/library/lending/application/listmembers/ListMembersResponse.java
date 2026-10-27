package library.lending.application.listmembers;

import java.util.List;

/**
 * The response model: the members, one summary each.
 */
public record ListMembersResponse(List<MemberSummary> members) {
}
