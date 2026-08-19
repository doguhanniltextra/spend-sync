import { RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/config/queryClient.config'
import { router } from '@/routes/AppRoutes'
import { ToastContainer } from '@/components/feedback/Toast'

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <ToastContainer />
    </QueryClientProvider>
  )
}
