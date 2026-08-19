import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { organizationApi } from '../services/organizationApi'
import { useToastStore } from '@/components/feedback/Toast'
import { TIMING } from '@/constants/timing'
import type {
  UpdateUserRolesRequest,
  UpdateUserLegalEntitiesRequest,
  InviteSubAccountRequest,
  GenerateRequisitionerLinkRequest,
} from '@/types/organization.types'

export function useUserManagement() {
  const queryClient = useQueryClient()

  // 1. Users Query
  const usersQuery = useQuery({
    queryKey: ['organization', 'users'],
    queryFn: organizationApi.getUsers,
    staleTime: TIMING.query.staleTime,
  })

  // 2. Active Invitations Query
  const invitationsQuery = useQuery({
    queryKey: ['organization', 'invitations'],
    queryFn: organizationApi.getInvitations,
    staleTime: TIMING.query.staleTime,
  })

  // 3. Update Roles
  const updateRolesMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateUserRolesRequest }) =>
      organizationApi.updateUserRoles(id, payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Roles Updated',
        message: `Roles for ${data.fullName} have been updated.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'users'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Role Update Failed',
        message: err.response?.data?.message || err.message || 'Failed to update roles',
      })
    },
  })

  // 4. Update Legal Entities
  const updateEntitiesMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateUserLegalEntitiesRequest }) =>
      organizationApi.updateUserLegalEntities(id, payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Legal Entities Assigned',
        message: `Entity access permissions updated for ${data.fullName}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'users'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Entity Update Failed',
        message: err.response?.data?.message || err.message || 'Failed to update entity access',
      })
    },
  })

  // 5. Toggle User Status
  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      organizationApi.toggleUserStatus(id, active),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'info',
        title: 'User Status Updated',
        message: `User ${data.fullName} is now ${data.isActive ? 'Active' : 'Suspended'}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'users'] })
    },
  })

  // 6. Invite Sub-Account
  const inviteMutation = useMutation({
    mutationFn: (payload: InviteSubAccountRequest) => organizationApi.inviteSubAccount(payload),
    onSuccess: (data) => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Invitation Dispatched',
        message: `Invitation email sent to ${data.email}.`,
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'invitations'] })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Invitation Failed',
        message: err.response?.data?.message || err.message || 'Failed to send invitation',
      })
    },
  })

  // 7. Generate Requisitioner Link
  const linkMutation = useMutation({
    mutationFn: (payload: GenerateRequisitionerLinkRequest) =>
      organizationApi.generateRequisitionerLink(payload),
    onSuccess: () => {
      useToastStore.getState().addToast({
        type: 'success',
        title: 'Passkey Link Generated',
        message: 'Requisitioner registration passkey generated successfully.',
      })
    },
    onError: (err: any) => {
      useToastStore.getState().addToast({
        type: 'error',
        title: 'Link Generation Failed',
        message: err.response?.data?.message || err.message || 'Failed to generate link',
      })
    },
  })

  // 8. Revoke Invitation
  const revokeMutation = useMutation({
    mutationFn: (id: string) => organizationApi.revokeInvitation(id),
    onSuccess: () => {
      useToastStore.getState().addToast({
        type: 'warning',
        title: 'Invitation Revoked',
        message: 'The invitation token has been permanently invalidated.',
      })
      queryClient.invalidateQueries({ queryKey: ['organization', 'invitations'] })
    },
  })

  return {
    users:               usersQuery.data ?? [],
    isLoadingUsers:      usersQuery.isLoading,
    invitations:         invitationsQuery.data ?? [],
    isLoadingInvites:    invitationsQuery.isLoading,
    updateRoles:         updateRolesMutation.mutateAsync,
    isUpdatingRoles:     updateRolesMutation.isPending,
    updateLegalEntities: updateEntitiesMutation.mutateAsync,
    isUpdatingEntities:  updateEntitiesMutation.isPending,
    toggleUserStatus:    toggleStatusMutation.mutateAsync,
    inviteUser:          inviteMutation.mutateAsync,
    isInviting:          inviteMutation.isPending,
    generateLink:        linkMutation.mutateAsync,
    isGeneratingLink:    linkMutation.isPending,
    revokeInvitation:    revokeMutation.mutateAsync,
    refetchUsers:        usersQuery.refetch,
    refetchInvites:      invitationsQuery.refetch,
  }
}
